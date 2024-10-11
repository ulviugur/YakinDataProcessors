package com.langpack.process;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.TextFileReader;
import com.langpack.model.AnalysisWrapper;
import com.langpack.model.BookQuote;
import com.langpack.model.WordModel;
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

	private File wordUniverseFile = null;
	private File rootMapsFile = null;
	private File rootStatsFile = null;

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

	AnalysisWrapper analyzer = new AnalysisWrapper();

	TreeMap<String, BookQuote> quoteMap = new TreeMap<String, BookQuote>();

	int quoteMinSentenceLength;
	int quotePreSentences;
	int quotePostSentences;

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

		wordUniverseFile = new File(wordUniverseFilePathStr);
		rootMapsFile = new File(rootMapsFilePathStr);
		rootStatsFile = new File(rootStatsFilePathStr);

		mongoClient = MongoClients.create(String.format("mongodb://%s:%s", server, port));
		database = mongoClient.getDatabase(dbName);

		wordColl = database.getCollection(wordCollName);
		quoteColl = database.getCollection(bookCollName);
		linkColl = database.getCollection(linkCollName);

		quoteMinSentenceLength = Integer.parseInt(cfgReader.getValue("quote.MinSentenceLength", "1"));
		quotePreSentences = Integer.parseInt(cfgReader.getValue("quote.PreSentences", "1"));
		quotePostSentences = Integer.parseInt(cfgReader.getValue("quote.PostSentences", "1"));

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
	
	private void useFileForLookups(Document doc, TreeSet<String> dictWords) {
		String retval = null;
		
		String stcFileName = (String) doc.get("stcFile");
		File searchFile = new File(stcFilesDirectory, stcFileName);
		
		TextFileReader reader = new TextFileReader(searchFile);
		String content = reader.readFile();
		String[] contentArray = content.split("\\n");

		log4j.info("Searching words in file {}", stcFileName);
		
		// Go through each line
		for (int lineCount = 0; lineCount < contentArray.length; lineCount++) {
			String line = contentArray[lineCount];
			String atomline = GlobalUtils.cleanWord(line);
			
			log4j.info("Currline : {}", atomline);
			
			if (atomline.contains("akılcı")) {
				log4j.info("");
			}
			
			String[] words = atomline.split(" ");

			if (words.length < quoteMinSentenceLength) {
				continue;
			}
			// Go through each word
			for (int wordCount = 0; wordCount < words.length; wordCount++) {
				String word = words[wordCount];
				if (word == null || "".equals(word.trim())) {
					continue;
				}
				List<WordModel> result = analyzer.getWordAnalysis(word);

				// Go through each analysis
				for (Iterator<WordModel> iterator = result.iterator(); iterator.hasNext();) {
					WordModel wordModel = (WordModel) iterator.next();
					//String stem = wordModel.getStem();
					String root = wordModel.getRootWord();
					//String mappedWord = wordModel.getMappedWord();
					if (root != null) {
						if (dictWords.contains(root)) {
							// check if the surrounding sentences are OK
							String quoteResult = qualifyQuote(contentArray, lineCount, wordCount);
							if (quoteResult != null) { // if approved insert
								addtoFound(root, quoteResult, doc);
							}
						}
						break;
					}
				}
				if (retval != null) {
					break;
				}
			}
			if (retval != null) {
				break;
			}

		}

	}

	private String qualifyQuote(String[] contentArray, int lineCursor, int wordCursor) {
		StringBuilder retval = new StringBuilder();
		if ((lineCursor - quotePreSentences < 0) || (lineCursor + quotePostSentences > contentArray.length)) {
			retval = null;
		} else {
			for (int i = lineCursor-quotePreSentences; i < lineCursor + quotePostSentences; i++) {
				String tmpSentence = contentArray[i];
				String[] tokens = tmpSentence.split(" ");

				if (tokens.length < quoteMinSentenceLength) {
					retval = null;
					break;
				}
				if (i == lineCursor) {
					String targetWord = tokens[wordCursor];
					String markedWord = "#" + targetWord + "#";
					log4j.info("Replacing {} with {}", targetWord, markedWord);
					String newSentence = tmpSentence.replace(targetWord, markedWord);
					retval.append(newSentence);
				} else {
					retval.append(tmpSentence);
				}

			}
		}
		if (retval == null) {
			return null;
		}
		return retval.toString();
	}

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

		wordSet.remove(_root);
		log4j.info("Found word {} in file {}", _root, stcFileName);
		log4j.info("Balance => Pending {} vs. Found {}", wordSet.size(), quoteMap.size());
	}
	
	public void process() {
		readAllWordsFromMongo();
		
		FindIterable<Document> iter = linkColl.find();
		MongoCursor<Document> cursor = iter.cursor();
		
		wordSet = new TreeSet<String>();
		wordSet.add("akılcı");
		while (cursor.hasNext()) {
			Document item = cursor.next();
			String stcFileName = (String)item.get("stcFile");
			File currentFile = new File(stcFilesDirectory, stcFileName);
			log4j.info("Running the file {} for quotes", currentFile.getAbsolutePath());
			useFileForLookups(item, wordSet);
			log4j.info("");
		}
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
		List<WordModel> dictWordCheck = analyzer.getWordAnalysis("açıklık");
		List<WordModel> bookWordCheck = analyzer.getWordAnalysis("açıklığa");
		
		List<WordModel> dictWordCheck2 = analyzer.getWordAnalysis("oturum");
		List<WordModel> bookWordCheck2 = analyzer.getWordAnalysis("oturumdayken");
		
		System.exit(-1);
		
		BookQuoteBatch batch = new BookQuoteBatch(configFilePath);

		batch.process();

	}
}
