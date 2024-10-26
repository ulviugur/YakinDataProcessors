package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.common.EndlessFileReader;
import com.langpack.common.GlobalUtils;
import com.langpack.model.AnalysisWrapper;
import com.langpack.model.AnalysisWrapper2;
import com.langpack.model.BookQuote;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class BookQuoteBatch {
	public static final Logger log4j = LogManager.getLogger("BookQuoteBatch");

	ConfigReader cfgReader = null;
	private String server = null;
	private String port = null;
	private String dbName = null;
	private String wordCollName = null;
	private String linkCollName = null;
	private String bookCollName = null;
	private String charactersToRemove = null;

	private String quoteLibraryPathStr = null;

	private String wordUniverseFilePathStr = null;
	private String rootMapsFilePathStr = null;
	private String rootStatsFilePathStr = null;

	File stcFilesDirectory = null;
	ArrayList<File> stcFiles = null;

	MongoClient mongoClient = null;
	MongoDatabase database = null;
	MongoCollection<Document> wordColl = null;
	MongoCollection<Document> linkColl = null;
	MongoCollection<Document> quoteColl = null;

	TreeSet<String> wordSet = new TreeSet<String>();
	TreeSet<String> phraseSet = new TreeSet<String>();

	TreeMap<String, TreeSet<String>> sampleIndex = new TreeMap<String, TreeSet<String>>();
	TreeMap<String, Integer> rootStats = new TreeMap<String, Integer>();

	AnalysisWrapper2 analyzer = new AnalysisWrapper2();

	TreeMap<String, BookQuote> quoteMap = new TreeMap<String, BookQuote>();

	HashMap<String, EndlessFileReader> stcMap = new HashMap<String, EndlessFileReader>();

	Iterator<String> stcReaderIter = null;

	EndlessFileReader currReader = null;

	int quoteMinSentenceLength;
	int quoteMaxSentenceLength;
	int quotePreSentences;
	int quotePostSentences;
	int skipLines;

	private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZçÇğĞıİöÖşŞüÜ";

	public BookQuoteBatch(String cfgFilePath) {

		cfgReader = new ConfigReader(cfgFilePath);
		server = cfgReader.getValue("mongo.Server", "localhost");
		port = cfgReader.getValue("mongo.Port", "27017");
		dbName = cfgReader.getValue("mongo.DBName", "test");
		wordCollName = cfgReader.getValue("mongo.WordCollName", "words");
		bookCollName = cfgReader.getValue("mongo.QuoteCollName", "GoldQuotes");
		linkCollName = cfgReader.getValue("mongo.LinksCollName", "GoldLinks");

		quoteLibraryPathStr = cfgReader.getValue("quoteLibrary.Directory", "");

		charactersToRemove = cfgReader.getValue("quoteLibrary.CharactersToRemove", "");

		if ("".equals(quoteLibraryPathStr)) {
			log4j.info("quoteLibrary.Directory is not set ! Cannot proceed !");
			log4j.info("Exitting !!");
			System.exit(-1);
		}

		stcFilesDirectory = new File(quoteLibraryPathStr);
		if (stcFilesDirectory.exists()) {
			if (stcFilesDirectory.isDirectory()) {
				log4j.info("Using {} as STC Library ..", quoteLibraryPathStr);
			} else {
				log4j.error("quoteLibrary.Directory {} is not a directory ! Cannot proceed !", quoteLibraryPathStr);
				log4j.info("Exitting !!");
				System.exit(-1);
			}
		} else {
			log4j.error("quoteLibrary.Directory {} does not exist ! Cannot proceed !", quoteLibraryPathStr);
			log4j.info("Exitting !!");
			System.exit(-1);
		}

		wordUniverseFilePathStr = cfgReader.getValue("export.WordUniverseFile", "");
		rootMapsFilePathStr = cfgReader.getValue("export.RootMapsFile", "");
		rootStatsFilePathStr = cfgReader.getValue("export.RootStatsFile", "");

		mongoClient = MongoClients.create(String.format("mongodb://%s:%s", server, port));
		database = mongoClient.getDatabase(dbName);

		wordColl = database.getCollection(wordCollName);
		quoteColl = database.getCollection(bookCollName);
		linkColl = database.getCollection(linkCollName);

		quoteMinSentenceLength = Integer.parseInt(cfgReader.getValue("quote.MinSentenceLength", "1"));
		quoteMaxSentenceLength = Integer.parseInt(cfgReader.getValue("quote.MaxSentenceLength", "20"));
		quotePreSentences = Integer.parseInt(cfgReader.getValue("quote.PreSentences", "1"));
		quotePostSentences = Integer.parseInt(cfgReader.getValue("quote.PostSentences", "1"));
		skipLines = Integer.parseInt(cfgReader.getValue("quoteLibrary.skipLines", "10"));

		// load all book info
		registerFilesFromSTCDirectory();

		// load all words
		// readAllWordsFromMongo();
		wordSet.add("kapı");

		// start from the first book file
		stcReaderIter = stcMap.keySet().iterator();
	}

	private void readAllWordsFromMongo() {
		MongoCursor<Document> cursor = wordColl.find().iterator();
		try {
			int count = 1;
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String word = doc.getString("word"); // log4j.info("[{}] word : {}", count, word);
				String[] parts = word.split("\\s+");
				if (parts.length > 1) {
					phraseSet.add(word);
					// log4j.info("Skipping \"{}\" for matching as it is a phrase !", word);
					if (!word.contains("(")) {
						// log4j.info("Phrase \"{}\" contains alternative phrases !", word);
					}
				} else {
					wordSet.add(word);
				}

				if ((wordSet.size() % 100 == 0) || (count % 1000 == 0)) {
					// log4j.info("Loaded {} words ", wordSet.size());
				}
				count++;
			}
			log4j.info("Loaded {} words ", wordSet.size());
			log4j.info("Founds {} phrases ", phraseSet.size());
		} finally {
			cursor.close();
		}

		// mongoClient.close();
	}

	private void registerFilesFromSTCDirectory() {
		FindIterable<Document> iter = linkColl.find();
		MongoCursor<Document> cursor = iter.cursor();

		while (cursor.hasNext()) {
			Document item = cursor.next();
			String stcFileName = (String) item.get("stcFile");
			File stcFile = new File(stcFilesDirectory, stcFileName);
			log4j.info("Registering file {} for quotes", stcFile.getAbsolutePath());
			EndlessFileReader er = null;
			try {
				er = new EndlessFileReader(stcFile, item, quotePreSentences, quotePostSentences, skipLines, false);
				stcMap.put(stcFileName, er);
			} catch (FileNotFoundException e) {
				log4j.info("File {} cound not be opened, skipping to take on the register ..", stcFile.getAbsolutePath());
			}

		}
	}

	public String searchQuote(ArrayList<String> contentArray, String searchWord) {
		StringBuilder retval = new StringBuilder();
		boolean found = false;

		for (int stcCount = 0; stcCount < quotePreSentences + quotePostSentences + 1; stcCount++) {
			String tmpSentence = contentArray.get(stcCount);
			String[] tokens = tmpSentence.split(" ");

			if (stcCount == quotePreSentences) { // look in the target sentence
				// if the target sentence is too short, jump out
				if (tokens.length < quoteMinSentenceLength || tokens.length > quoteMaxSentenceLength) {
					retval = null;
					break;
				}
				for (int wordCount = 0; wordCount < tokens.length; wordCount++) {
					String targetWord = tokens[wordCount];
					TreeSet<String> derivedList = analyzer.getDerivedWords(targetWord);
					if (derivedList.contains(searchWord)) {
						found = true;
						String markedWord = "#" + targetWord + "#";
						log4j.info("Replacing {} with {}", targetWord, markedWord);
						tokens[wordCount] = markedWord;
					}
				}
				retval.append(" ");
				retval.append(GlobalUtils.convertArraytoString(tokens, " "));

			} else {
				retval.append(" ");
				retval.append(tmpSentence);
			}

		}

		if (!found) {
			return null;
		} else {
			return retval.toString().trim();
		}
	}

//	private String qualifyQuote(String[] contentArray, int lineCursor, int wordCursor) {
//		StringBuilder retval = new StringBuilder();
//		if ((lineCursor - quotePreSentences < 0) || (lineCursor + quotePostSentences > contentArray.length)) {
//			retval = null;
//		} else {
//			for (int i = lineCursor-quotePreSentences; i < lineCursor + quotePostSentences; i++) {
//				String tmpSentence = contentArray[i];
//				String[] tokens = tmpSentence.split(" ");
//
//				if (tokens.length < quoteMinSentenceLength) {
//					retval = null;
//					break;
//				}
//				if (i == lineCursor) {
//					String targetWord = tokens[wordCursor];
//					String markedWord = "#" + targetWord + "#";
//					log4j.info("Replacing {} with {}", targetWord, markedWord);
//					String newSentence = tmpSentence.replace(targetWord, markedWord);
//					retval.append(newSentence);
//				} else {
//					retval.append(tmpSentence);
//				}
//
//			}
//		}
//		if (retval == null) {
//			return null;
//		}
//		return retval.toString();
//	}

	private void addtoFound(String _root, String _currline, Document doc) {
		String key = (String) doc.get("key");
		String stcFileName = (String) doc.get("stcFile");
		String bookTitle = (String) doc.get("bookTitle");
		String author = (String) doc.get("author");

		BookQuote data = new BookQuote();
		data.setWord(_root);
		data.setQuote(_currline);
		data.setKey(key);
		data.setBookTitle(bookTitle);
		data.setAuthor(author);

		// insert for testing purposes when found
		insertOneQuoteRecord(data);

		log4j.info("Found word {} in file {}", _root, stcFileName);
		log4j.info("Balance => Pending {} vs. Found {}", wordSet.size(), quoteMap.size());
	}

	public void process() {
		Iterator<String> wordIter = null;
		ArrayList<String> paragraph = null;
		String quoteResult = null;
		int maxFileSearch = stcMap.size() + 1; // check all files not more
		int fileCount = 0;
		EndlessFileReader reader = null;
		boolean currentFileWasGood = true; // that means, dont skip this file
		while (true) {
			reader = getNextReader();

			while (true) { // roll the cache until the minsentencelength criteria is filled
				paragraph = reader.rollCache();
				String targetStc = paragraph.get(quotePreSentences);
				String[] tokens = targetStc.split(" ");
				if (tokens.length >= quoteMinSentenceLength) {
					break;
				}
			}

			log4j.info(GlobalUtils.convertArraytoString(paragraph, " "));

			wordIter = wordSet.iterator();
			String word = null;
			currentFileWasGood = false;
			while (wordIter.hasNext()) {
				word = wordIter.next();
				// log4j.info("Processing word : {}", word);

				quoteResult = searchQuote(paragraph, word);
				if (quoteResult != null) { // if approved insert
					addtoFound(word, quoteResult, reader.getFileProps());
					wordSet.remove(word); // since we are jumping out of the loop, illegal state is not an issue
					fileCount = 0;
					currentFileWasGood = true;
					break;
				}
			}

			if (fileCount >= maxFileSearch) { // if you searched through all files
				break;
			}
			fileCount++;
		}

	}

	public EndlessFileReader getNextReader() {
		if (!stcReaderIter.hasNext()) {
			stcReaderIter = stcMap.keySet().iterator();
		}
		String nextFileKey = stcReaderIter.next();
		currReader = stcMap.get(nextFileKey);

		return currReader;
	}

	public void insertOneQuoteRecord(BookQuote data) {
		data.setShowDate(Date.from(Instant.now()));
		Document record = convertToDocument(data);
		quoteColl.insertOne(record);
		log4j.info("");
	}

	public org.bson.Document convertToDocument(BookQuote data) {
		org.bson.Document document = new org.bson.Document(); // Create a new BSON Document
		for (Field field : BookQuote.class.getDeclaredFields()) {
			field.setAccessible(true); // Allow access to private fields
			try {
				Object value = field.get(data);
				if (value != null) {
					document.append(field.getName(), value); // Populate the Document with fields
				}
			} catch (IllegalAccessException e) {
				System.err.println("Error accessing field: " + e.getMessage());
			}
		}
		return document; // Return the BSON Document
	}

	public static void main(String[] args) {

		String configFilePath = args[0];

		AnalysisWrapper analyzer = new AnalysisWrapper();
//		List<WordModel> dictWordCheck = analyzer.getWordAnalysis("açıklık");
//		List<WordModel> bookWordCheck = analyzer.getWordAnalysis("açıklığa");
//
//		List<WordModel> dictWordCheck2 = analyzer.getWordAnalysis("oturum");
//		List<WordModel> bookWordCheck2 = analyzer.getWordAnalysis("oturumdayken");

		BookQuoteBatch instance = new BookQuoteBatch(configFilePath);
//		ArrayList<String> content = new ArrayList<String> (); 
//		content.add("Hiçbiri bize yakın oturmuyordu.");
//		content.add("Mahallemize kara karalı kartallar konmuştu.");
//		content.add("Gündelik işlerimizi kendimiz yaptık.");
//		content.add("Artık bir değişikliğin zamanı gelmişti.");
//		
//		String res = instance.searchQuote(content, "kara");
//		
//		log4j.info("");

		instance.process();

	}
}
