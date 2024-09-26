package com.langpack.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.BasicClass;
import com.langpack.common.FileExporter;
import com.langpack.common.FileLedger;
import com.langpack.common.GlobalUtils;
import com.langpack.common.KeyList;
import com.langpack.scraper.ChromeHeadless;
import com.langpack.scraper.ChromeWorker;
import com.langpack.scraper.DictWord;
import com.langpack.scraper.HtmlTagList;
import com.langpack.scraper.TokenList;
import com.langpack.scraper.TurkishCharMapper;
import com.langpack.structure.WordGenerator;

// Read diff records from the database, scrape them and insert into another table
public class LedgerManager extends BasicClass {
	public static final Logger log4j = LogManager.getLogger("LedgerManager");

	String tableNames = null;
	String[] tableList = null;

	String parserNames = null;
	String[] parserList = null;

	String htmlTagFileName = null;
	HtmlTagList htmlTagList = new HtmlTagList();

	String tokensFileName = null;

	String exportFileName = null;
	FileExporter exporter = null;

	String TurkishCharsMappingFile = null;
	String TDKScrapeConfigFileName = null;
	TurkishCharMapper charMapper = null;
	String ChromeHeadlessConfigFile = null;

	TreeSet<String> wordList = new TreeSet<>();

	TDKScraper scraper = null;

	TreeSet<String> postalCodeList = new TreeSet<>();

	ChromeHeadless chrome = null;
	WordGenerator generator = null;

	Integer focus = -1;
	Boolean CREATE_FOCUS_ITEMS = false; // if this is enabled, all tasks are distributed to the workers in the
										// QUEUE_LEDGER Table
	int WORKER_SIZE = 4;

	public LedgerManager(String cfgFileName) {
		super(cfgFileName);

		focus = Integer.parseInt(cfg.getValue("Focus"));
		CREATE_FOCUS_ITEMS = Boolean.parseBoolean(cfg.getValue("CreateFocusItems"));
		WORKER_SIZE = Integer.parseInt(cfg.getValue("WorkerSize"));

		htmlTagFileName = cfg.getValue("HtmlTagsFileName");
		htmlTagList.readTagsFromTextFile(htmlTagFileName);

		tokensFileName = cfg.getValue("TokensFileName");
		TokenList.readTagsFromTextFile(tokensFileName);

		TurkishCharsMappingFile = cfg.getValue("TurkishCharMappingFile");
		charMapper = new TurkishCharMapper(TurkishCharsMappingFile);

		TDKScrapeConfigFileName = cfg.getValue("TDKScrapeConfigFile");
		ChromeHeadlessConfigFile = cfg.getValue("ChromeHeadlessConfigFile");

		tableNames = cfg.getValue("TableNames");
		tableList = tableNames.replaceAll("\\s+", "").split(",");

		parserNames = cfg.getValue("ParserNames");
		parserList = parserNames.replaceAll("\\s+", "").split(",");

		exportFileName = cfg.getValue("WordExportFile");
		try {
			exporter = new FileExporter(exportFileName, "UTF-8");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	// extract all words in a given table using the given parser for level2 (both
	// defined by the configuration file)
	// add them all to #wordList#
	public void scanTablesforWords() {
		ResultSet records = null;
		int recordCount = 0;

		for (int tableCount = 0; tableCount < parserList.length; tableCount++) {
			String tableName = tableList[tableCount];
			Level2Parser parser = null;

			try {
				parser = (Level2Parser) Class.forName("com.langpack.scraper." + parserList[tableCount]).newInstance();
			} catch (InstantiationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String realQueryString = null;
			try {
				realQueryString = selectString1.replaceAll("#TABLE#", tableName);

				psSelect1 = DBconn.prepareStatement(realQueryString);
				records = psSelect1.executeQuery();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				while (records.next()) {
					String word = records.getString(1);
					// String source = records.getString(2);
					// String id = records.getString(3);
					String level2 = records.getString(2);
					if (!level2.contains("Sonuç bulunamadı")) { // TDK sözlügünde bulunamamis kayit, ici bostur
						TreeSet<String> words4 = cleanseWordlist(level2, parser);
						wordList.addAll(words4);
					}

					log4j.info(String.format("Table:%s, RecordCount: %s, size: %s", tableCount, recordCount,
							wordList.size()));
					recordCount++;
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// clean level2 content with the relevant parser
	public TreeSet<String> cleanseWordlist(String level2, Level2Parser parser) {
		// logger.info(level2);
		TreeSet<String> retval = null;
		String level2_1 = charMapper.replaceCodedCharacters(level2);
		String level2_2 = parser.removeUnwantedKeywords(level2_1);
		parser.setLevel2(level2_2);
		log4j.info(level2_2);

		TreeSet<String> words = parser.getWordsAsCollection();
		log4j.info(GlobalUtils.convertArraytoString(words, ";"));
		TreeSet<String> words2 = htmlTagList.removeHTMLTags(words);
		// logger.info(GlobalUtils.convertArraytoString(words2, ";"));
		TreeSet<String> words3 = TokenList.removeTokens(words2);
		// logger.info(GlobalUtils.convertArraytoString(words3, ";"));
		retval = parser.postProcess(words3);
		// logger.info(GlobalUtils.convertArraytoString(retval, ";"));
		return retval;
	}
	// Full dictionary created ..
	// Lookup in the Ledger and TDK tables, insert if not known

	public void saveWordListIntoLedgerTable(TreeSet<String> importList, String source) {
		int processCount = 0;

		int fullCount = importList.size();

		for (String word : importList) {
			scraper.lookupWordinTDKAndInsert(word, source, false);
			log4j.info(String.format("[%s/%s] Processed word : %s", processCount, fullCount, word));
			processCount++;
		}
	}

	public void lookupInTDK() {
		// run through unprocessed ledger records for lookup
		ResultSet records = null;

		int recordCount = 0;
		try {
			records = psSelect2.executeQuery();
			while (records.next()) {
				String id = records.getString(1);
				String word = records.getString(2);
				String source = records.getString(3);

				DictWord wordObject = scraper.lookupWordinTDKAndInsert(word, source, false);

				recordCount++;
				log4j.info(String.format("[%s] Updated record id: %s, %s: ", recordCount, id, word));

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * public synchronized void addToLedger(String word, String keyNew) {
	 *
	 * try { psInsert1.setString(1, word); psInsert1.setString(2, keyNew);
	 *
	 * java.util.Date time = new java.util.Date();
	 *
	 * psInsert1.setString(3, format.format(time)); try { psInsert1.executeUpdate();
	 * } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
	 * log4j.info(String.format("%s is already in the TDK_SCRAPE table", word)); }
	 * catch (Exception ex) { ex.printStackTrace(); } } catch (SQLException e) { //
	 * TODO Auto-generated catch block e.printStackTrace(); }
	 *
	 * }
	 */

	public void runThroughLedgerAny(int focus) { // focus means number of characters which make a key to be checked
													// after.
		Locale turkishLoc = new Locale("tr", "TR");
		int KEY_LENGTH = focus;
		ResultSet records = null;

		KeyList keyList = new KeyList();
		int count = 0;
		try {
			records = psSelect1.executeQuery();

			while (records.next()) {
				String word = records.getString(1);

				String key = word.toLowerCase(turkishLoc); // no need to .trim() as it would remove the possibility of
															// multi-word entries
				keyList.addKeyOccurence(key);
				count++;
				log4j.info(String.format("Item:[%s], recordCount:[%s], listCount:[%s]", key, count, keyList.getSize()));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info("Compiled a list of " + keyList.getSize() + " items ..");

		generator = new WordGenerator(KEY_LENGTH);
		File ledgerFile = new File(cfg.getValue("ScrapeLedgerFile"));
		if (!ledgerFile.exists()) {
			log4j.info("Ledger file does not exist, will initialize : " + ledgerFile.getAbsolutePath());
			boolean created = false;
			try {
				created = ledgerFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (created) {
				log4j.info("Ledger file created successfully");
			} else {
				log4j.error(String.format("Creating ledger file failed [%s], exitting !!"));
				System.exit(-1);
			}
		}

		FileLedger ledger = null;
		try {
			ledger = new FileLedger(cfg.getValue("ScrapeLedgerFile"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		keyList.getMinOccurences(6); // remove all records less that 6 occurences as they would not lead to
										// uncaptured records
		Set<String> keySet = keyList.getKeys();

		chrome = new ChromeHeadless(ChromeHeadlessConfigFile);

		// run through unprocessed ledger records for lookup
		TreeSet<String> lookupList = new TreeSet<>();
		while (true) {
			generator.nextWord();
			String value = generator.toString();
			if (value == null) {
				log4j.info("Reached the end ..");
				break;
			} else {
				// compare generated key with previously proven (length-1) keys
				String compKey = value.substring(0, value.length() - 1);
				if (keySet.contains(compKey)) {
					lookupList.add(value);
				}
			}
			if ((lookupList.size() % 10) == 0) {
				// log4j.info(String.format("Reached %s keys", lookupList.size()));
			}
		}
		log4j.info(String.format("Loaded %s keys from the word generator", lookupList.size()));

		TreeSet<String> fullList = new TreeSet<>();
		int recordCount = 1;

		boolean DRY_RUN = false;
		for (String keyNew : lookupList) {
			String keyShort = keyNew.substring(0, KEY_LENGTH - 1);
			if (ledger.isInledger(keyNew)) {
				log4j.info("Skipping word : " + keyNew + ", already in ledger (lookedup) ..");
			} else if (!keySet.contains(keyShort)) {
				// if short key (3 characters) is not known, this key does not have any list at
				// all
				log4j.info("Skipping word : " + keyNew + ", this is not a known key ..");
			} else {
				log4j.info("Looking for the key : _" + keyNew + "_");
				try {
					ArrayList<String> words = chrome.scrapeTDKWordsWithKey(keyNew);

					for (String word : words) {
						if (word != null) {
							if (!DRY_RUN) {
								psInsert1.setString(1, word);
								psInsert1.setString(2, keyNew);

								java.util.Date time = new java.util.Date();

								psInsert1.setString(3, format.format(time));
								try {
									psInsert1.executeUpdate();
								} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
									log4j.info(String.format("%s is already in the TDK_SCRAPE table", word));
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}
					fullList.addAll(words);
					String values = GlobalUtils.convertArraytoString(words);
					if (words.size() > 0) {
						log4j.info("Found entries :" + values);
					}

					String msg = String.format("Total keys [%s] -> Found words [%s] ", recordCount, fullList.size());
					log4j.info(msg);
					if (!DRY_RUN) {
						ledger.addtoLedger(keyNew);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				recordCount++;
			}
		}
		ledger.closeLedger();
	}

	public void runThroughFocus() { // focus means number of characters which make a key to be checked after. 3 is
									// used 4,5 can also be used to verify holes

		Integer ROUND_ROBIN = 0; // assign task to workers in a round-robin

		WordGenerator gen = new WordGenerator(focus);

		TreeMap<Integer, ChromeWorker> workerArray = new TreeMap<>();
		TreeMap<Integer, Thread> threadArray = new TreeMap<>();

		for (Integer i = 0; i < WORKER_SIZE; i++) {
			ChromeWorker queWorker = new ChromeWorker(cfg.getValue("QueueLedgerConfigFile"));
			workerArray.put(i, queWorker);
			Thread t1 = new Thread(queWorker);
			threadArray.put(i, t1);
			t1.start();
		}

		if (CREATE_FOCUS_ITEMS) {
			String keyNew = gen.nextWord();
			do {
				log4j.info("Adding key to the Queue : _" + keyNew + "_");
				try {
					ChromeWorker tmpWorker = workerArray.get(ROUND_ROBIN);
					tmpWorker.addToQueueTable(keyNew, keyNew);
					ROUND_ROBIN++;
					if (ROUND_ROBIN > WORKER_SIZE - 1) {
						ROUND_ROBIN = 0;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} while ((keyNew = gen.nextWord()) != null);
		}

		for (Integer threadId : threadArray.keySet()) {
			Thread thread = threadArray.get(threadId);
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		LedgerManager instance = new LedgerManager(args[0]);
		instance.runThroughFocus();

		// instance.scanTablesforWords();
		// instance.saveWordListingDB();

	}

}
