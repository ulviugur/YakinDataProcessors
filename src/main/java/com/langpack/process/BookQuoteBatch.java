package com.langpack.process;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.common.TextFileReader;
import com.langpack.model.AnalysisWrapper;
import com.langpack.model.BookQuoteData;
import com.langpack.model.WordModel;
import com.mongodb.MongoClient;
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
	private String bookCollName = null;
	private String charactersToRemove = null;

	private String quoteLibraryPathStr = null;

	private String wordUniverseFilePathStr = null;
	private String rootMapsFilePathStr = null;
	private String rootStatsFilePathStr = null;

	private File wordUniverseFile = null;
	private File rootMapsFile = null;
	private File rootStatsFile = null;

	File quoteLibraryPath = null;
	File[] quoteFiles = null;

	MongoClient mongoClient = null;
	MongoDatabase database = null;
	MongoCollection<Document> wordCollection = null;
	MongoCollection<Document> quoteCollection = null;

	TreeSet<String> wordSet = new TreeSet<String>();
	TreeSet<String> phraseSet = new TreeSet<String>();

	TreeMap<String, TreeSet<String>> sampleIndex = new TreeMap<String, TreeSet<String>>();
	TreeMap<String, Integer> rootStats = new TreeMap<String, Integer>();

	AnalysisWrapper analyzer = new AnalysisWrapper();

	TreeMap<String, BookQuoteData> quoteMap = new TreeMap<String, BookQuoteData>();

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
		bookCollName = cfgReader.getValue("mongo.QuoteCollName", "bookquote");

		quoteLibraryPathStr = cfgReader.getValue("quoteLibrary.Directory", "");

		charactersToRemove = cfgReader.getValue("quoteLibrary.CharactersToRemove", "");

		if ("".equals(quoteLibraryPathStr)) {
			log4j.info("quoteLibrary.Directory is not set ! Cannot proceed !");
			log4j.info("Exitting !!");
			System.exit(-1);
		}

		quoteLibraryPath = new File(quoteLibraryPathStr);
		if (quoteLibraryPath.exists()) {
			if (quoteLibraryPath.isDirectory()) {
				log4j.info("Using {} as BookLibrary ..", quoteLibraryPathStr);
				quoteFiles = quoteLibraryPath.listFiles();
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

		mongoClient = new MongoClient(server, Integer.parseInt(port));
		database = mongoClient.getDatabase(dbName);

		wordCollection = database.getCollection(wordCollName);
		quoteCollection = database.getCollection(bookCollName);

		quoteMinSentenceLength = Integer.parseInt(cfgReader.getValue("quote.MinSentenceLength", "1"));
		quotePreSentences = Integer.parseInt(cfgReader.getValue("quote.PreSentences", "1"));
		quotePostSentences = Integer.parseInt(cfgReader.getValue("quote.PostSentences", "1"));

	}

	public void readAllWordsFromMongo() {
		MongoCursor<Document> cursor = wordCollection.find().iterator();
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

	private void useFileForLookups(File searchFile, TreeSet<String> dictWords) {
		String retval = null;
		TextFileReader reader = new TextFileReader(searchFile);
		String content = reader.readFile();
		String[] contentArray = content.split("\\n");

		// Go through each line
		for (int lineCount = 0; lineCount < contentArray.length; lineCount++) {
			String currline = contentArray[lineCount];
			String[] words = currline.split(" ");

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
					String stem = wordModel.getStem();
					String root = wordModel.getRootWord();
					if (root != null) {
						if (dictWords.contains(root)) {
							// check if the surrounding sentences are OK
							String quoteResult = qualifyQuote(contentArray, lineCount, wordCount);
							if (quoteResult != null) { // if approved insert
								addtoFound(root, quoteResult, searchFile.getName());
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

	private void addtoFound(String _root, String _currline, String _filename) {
		BookQuoteData data = new BookQuoteData();
		data.setWord(_root);
		data.setBookTitle(_filename);
		data.setQuote(_currline);
		quoteMap.put(_root, data);

		// insert for testing purposes when found
		insertOneQuoteRecord(data);

		wordSet.remove(_root);
		log4j.info("Found word {} in file {}", _root, _filename);
		log4j.info("Balance => Pending {} vs. Found {}", wordSet.size(), quoteMap.size());
	}

	/*
	 * private String lookupWordInFile(File searchFile, String _word) { String
	 * retval = null; String line = null; TextFileReader reader = new
	 * TextFileReader(searchFile);
	 * 
	 * try { while ((line = reader.readLine()) != null) { String[] words =
	 * line.split(" "); for (int i = 0; i < words.length; i++) { String word =
	 * words[i]; List<WordModel> result = analyzer.getWordAnalysis(word); for
	 * (Iterator<WordModel> iterator = result.iterator(); iterator.hasNext();) {
	 * WordModel wordModel = (WordModel) iterator.next(); String stem =
	 * wordModel.getStem(); String root = wordModel.getRootWord(); if (root != null
	 * && root.equals(_word)) { if (_word.contains("erildi.")) { log4j.info(""); }
	 * retval = line; log4j.info("Found word {} in file {}", _word,
	 * searchFile.getName()); break; } } if (retval != null) {break;} } if (retval
	 * != null) {break;} } } catch (IOException e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } return retval; }
	 */

	public void process() {
		readAllWordsFromMongo();
		for (int i = 0; i < quoteFiles.length; i++) {
			File currentFile = quoteFiles[i];
			log4j.info("Running the file {} for quotes", currentFile.getAbsolutePath());
			useFileForLookups(currentFile, wordSet);
		}
		/*
		 * Iterator<String> wordIter = quoteMap.keySet().iterator(); while
		 * (wordIter.hasNext()) { String wordKey = (String) wordIter.next();
		 * BookQuoteData data = quoteMap.get(wordKey); insertOneQuoteRecord(data); }
		 */

	}

	public void insertOneQuoteRecord(BookQuoteData data) {
		data.setShowDate(Date.from(Instant.now()));
		Document record = convertToDocument(data);
		quoteCollection.insertOne(record);
	}

	public org.bson.Document convertToDocument(BookQuoteData data) {
		org.bson.Document document = new org.bson.Document(); // Create a new BSON Document
		for (Field field : BookQuoteData.class.getDeclaredFields()) {
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
		BookQuoteBatch batch = new BookQuoteBatch(configFilePath);

		batch.process();

	}
}
