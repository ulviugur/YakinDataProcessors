package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.common.TextFileReader;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.langpack.model.AnalysisWrapper;
import com.langpack.model.WordModel;

public class FileBasedBookContentProcessing {
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
	MongoCollection<Document> bookCollection = null;

	TreeSet<String> wordSet = new TreeSet<String>();

	TreeMap<String, TreeSet<String>> sampleIndex = new TreeMap<String, TreeSet<String>>();
	TreeMap<String, Integer> rootStats = new TreeMap<String, Integer>();

	AnalysisWrapper analyzer = new AnalysisWrapper();

	private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZçÇğĞıİöÖşŞüÜ ";

	// This process is used in multiple stages :
	// 1- to read all books and create a "dirty" word list
	// 2- clean the dirty lists into usable ("clean") words
	// 3- collect a list of root words which are behind the clean words
	
	public FileBasedBookContentProcessing(String cfgFilePath) {
		cfgReader = new ConfigReader(cfgFilePath);
		server = cfgReader.getValue("mongo.Server", "localhost");
		port = cfgReader.getValue("mongo.Port", "27017");
		dbName = cfgReader.getValue("mongo.DBName", "test");
		wordCollName = cfgReader.getValue("mongo.WordCollName", "words");
		bookCollName = cfgReader.getValue("mongo.BookCollName", "bookquote");

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

		mongoClient = MongoClients.create(String.format("mongodb://%s:%s", server, port));
		database = mongoClient.getDatabase(dbName);

		wordCollection = database.getCollection(wordCollName);
		bookCollection = database.getCollection(bookCollName);
	}

	// Add each word a rating so that we identify the frequent / seldom axis of each
	// word
	// This should be run infrequently to add / correct / update the word ratings
	public void addWordRatings() {
		MongoCursor<Document> cursor = wordCollection.find().iterator();
		try {
			int count = 1;
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String word = doc.getString("word");
				log4j.info("[{}] word : {}", count, word);
				wordSet.add(word);
				count++;
			}
		} finally {
			cursor.close();
		}
	}

	public void collectWordInBooks() {

		TreeSet<String> wordsUniverse = new TreeSet<String>();

		int retval = 0;
		for (int fileCount = 0; fileCount < quoteFiles.length; fileCount++) {

			File tmpFile = quoteFiles[fileCount];
			log4j.info("[{}] Analyzing file : {}", fileCount, tmpFile.getAbsolutePath());
			TextFileReader quoteReader = new TextFileReader(tmpFile);
			String line = quoteReader.readFile();
			String[] sentenceWords = line.split("\\s+");
			for (int j = 0; j < sentenceWords.length; j++) {
				String currWord = sentenceWords[j];
				String matchableWord = currWord.replaceAll(charactersToRemove, "");
				if (!"".equals(matchableWord)) {
					wordsUniverse.add(matchableWord);
				}
			}
		}

		log4j.info("Exporting universe file : {}", wordUniverseFile.getAbsolutePath());

		try {
			FileExporter wordsExporter = new FileExporter(wordUniverseFile);
			for (Iterator<String> iterator = wordsUniverse.iterator(); iterator.hasNext();) {
				String exportWord = (String) iterator.next();
				wordsExporter.writeLineToFile(exportWord);
			}
			wordsExporter.closeExportFile();

		} catch (FileNotFoundException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}

		TextFileReader reader = new TextFileReader(wordUniverseFile);
		TreeSet<String> rootMappingList = new TreeSet<String>();
		String wordline = null;
		try {
			while ((wordline = reader.readLine()) != null) {
				List<WordModel> result = analyzer.getWordAnalysis(wordline);
				for (Iterator<WordModel> iterator = result.iterator(); iterator.hasNext();) {
					WordModel wordModel = (WordModel) iterator.next();
					String stem = wordModel.getStem();
					String root = wordModel.getRootWord();
					if (stem == null || root == null) {

					} else {
						rootMappingList.add(root + "-" + stem);
						if (!stem.equals(root)) {
							// log4j.info("Stem : {}, Root : {}", stem, root);
						}
					}
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Exporting roots file : {}", rootMapsFile.getAbsolutePath());

		try {
			FileExporter rootsExporter = new FileExporter(rootMapsFile);
			for (Iterator<String> iterator = rootMappingList.iterator(); iterator.hasNext();) {
				String exportMap = (String) iterator.next();
				rootsExporter.writeLineToFile(exportMap);
			}

		} catch (FileNotFoundException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TreeSet<String> collectRootsFromFile(File tmpFile) {
		TreeSet<String> retval = new TreeSet<String>();

		log4j.info("Analyzing file : {}", tmpFile.getAbsolutePath());
		TextFileReader wordReader = new TextFileReader(tmpFile);
		String word = null;
		int lineCount = 0;
		try {
			while ((word = wordReader.readLine()) != null) {
				lineCount++;
				if (!"".equals(word)) {
					List<WordModel> result = analyzer.getWordAnalysis(word);
					for (Iterator<WordModel> iterator = result.iterator(); iterator.hasNext();) {
						WordModel wordModel = (WordModel) iterator.next();
						String stem = wordModel.getStem();
						String root = wordModel.getRootWord();
						if (root != null) {
							
							int oldSize = retval.size();
							retval.add(root);
							int newSize = retval.size();
							if (((retval.size() % 100) == 0 && !(oldSize == newSize)) || (lineCount % 1000 == 0)) {
								log4j.info("Processing {} wordlist, reached line {} ", retval.size(), lineCount);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public void collectRootStatsInQuotes() {
		// Run through all files
		for (int fileCount = 0; fileCount < quoteFiles.length; fileCount++) {

			File tmpFile = quoteFiles[fileCount];
			log4j.info("[{}] Analyzing file : {}", fileCount, tmpFile.getAbsolutePath());
			TextFileReader quoteReader = new TextFileReader(tmpFile);
			String line = quoteReader.readFile();
			String[] sentenceWords = line.split("\\s+");
			for (int j = 0; j < sentenceWords.length; j++) {
				String currWord = sentenceWords[j];
				String matchableWord = currWord.replaceAll(charactersToRemove, "");
				if (!"".equals(matchableWord)) {
					List<WordModel> result = analyzer.getWordAnalysis(matchableWord);
					for (Iterator<WordModel> iterator = result.iterator(); iterator.hasNext();) {
						WordModel wordModel = (WordModel) iterator.next();
						String stem = wordModel.getStem();
						String root = wordModel.getRootWord();
						if (root == null) {

						} else {
							addTickToRoot(root);
						}
					}
				}
			}
		}

	}

	public TreeSet<String> collectRawWordsFromBooks() {

		TreeSet<String> wordsUniverse = new TreeSet<String>();

		for (int fileCount = 0; fileCount < quoteFiles.length; fileCount++) {

			File tmpFile = quoteFiles[fileCount];
			log4j.info("[{}] Analyzing file : {}", fileCount, tmpFile.getAbsolutePath());
			TextFileReader quoteReader = new TextFileReader(tmpFile);
			String line = quoteReader.readFile();
			String[] sentenceWords = line.split("\\s+");
			for (int j = 0; j < sentenceWords.length; j++) {
				String iniWord = sentenceWords[j];
				String currWord = cleanWord(iniWord);
				if (!"".equals(currWord)) {
					int oldSize = wordsUniverse.size();
					wordsUniverse.add(currWord);
					int newSize = wordsUniverse.size();
					if ((wordsUniverse.size() % 1000) == 0 && !(oldSize == newSize)) {
						log4j.info("Processing {} raw wordlist", wordsUniverse.size());
					}
				}

			}
		}
		return wordsUniverse;
	}

	public static String cleanWord(String word) {
		// Use StringBuilder for efficient string manipulation
		StringBuilder cleanedWord = new StringBuilder();

		// Iterate over each character in the input word
		for (char ch : word.toCharArray()) {
			// Append to cleanedWord if the character is in the allowed set
			if (ALLOWED_CHARACTERS.indexOf(ch) != -1) {
				cleanedWord.append(ch);
			}
		}

		// Return the cleaned word as a string
		return cleanedWord.toString();
	}

	public void writeWordsFile(TreeSet<String> wordsList, File exportFile) {
		log4j.info("Exporting words to file : {}", exportFile.getAbsolutePath());
		FileExporter exporterWords = null;
		try {
			exporterWords = new FileExporter(exportFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Iterator<String> wordIterator = wordsList.iterator();
		while (wordIterator.hasNext()) {
			String word = wordIterator.next();
			String exportLine = word;
			exporterWords.writeLineToFile(exportLine);
		}
		exporterWords.closeExportFile();
		log4j.info("Completed writing words file, wrote {} lines.", wordsList.size());
	}

	public void writeRootStatsFile() {
		log4j.info("Exporting stats to file : {}", rootStatsFile.getAbsolutePath());
		FileExporter exportRootStats = null;
		try {
			exportRootStats = new FileExporter(rootStatsFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Iterator<String> rootIterator = rootStats.keySet().iterator();
		while (rootIterator.hasNext()) {
			String rootWord = rootIterator.next();
			Integer stat = rootStats.get(rootWord);
			String exportLine = rootWord + "-" + stat.toString();
			exportRootStats.writeLineToFile(exportLine);
		}
		exportRootStats.closeExportFile();
		log4j.info("Completed exporting stats file, wrote {} lines.", rootStats.size());
	}

	private void addTickToRoot(String _word) {
		Integer currValue = rootStats.get(_word);
		if (currValue == null) {
			currValue = 0;
		}
		currValue++;
		int oldSize = rootStats.size();
		rootStats.put(_word, currValue);
		int newSize = rootStats.size();
		if ((rootStats.size() % 1000) == 0 && !(oldSize == newSize)) {
			log4j.info("Processing {} roots", rootStats.size());
		}
	}

	private void updateWordRating(Document filter, String value) {
		// Create an update statement to add the new field
		Document update = new Document("$set", new Document("reting", value));

		// Perform the update operation
		wordCollection.updateOne(filter, update);
	}

	public void process() {
		// Loading words into wordSet
		/*
		 * MongoCursor<Document> cursor = wordCollection.find().iterator(); try { int
		 * count = 1; while (cursor.hasNext()) { Document doc = cursor.next(); String
		 * word = doc.getString("word"); // log4j.info("[{}] word : {}", count, word);
		 * wordSet.add(word); count++; } } finally { cursor.close(); }
		 */
		// collectWordInBooks("");

		// collectRootStatsInQuotes();
		// writeRootStatsFile();

		// Collecting raw words from dirty books
		// TreeSet<String> wordList = collectRawWordsFromBooks();
		// writeWordsFile(wordList, wordUniverseFile);
		
		// Collect roots from the clean file
		TreeSet<String> rootWordsFromFile = collectRootsFromFile(wordUniverseFile);
		writeWordsFile(rootWordsFromFile, rootMapsFile);
		
		mongoClient.close();
	}

	public static void main(String[] args) {

		String configFilePath = args[0];
		FileBasedBookContentProcessing batch = new FileBasedBookContentProcessing(configFilePath);

		batch.process();

	}
}
