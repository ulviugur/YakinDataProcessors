package com.langpack.scraper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.BasicClass;
import com.langpack.integration.TDKChapterItem;
import com.langpack.integration.TDKMeaningItem;
import com.langpack.integration.TDKWordWrapper;
import com.langpack.structure.WordGenerator;

public class ChromeWorker extends BasicClass implements Runnable {

	public static final Logger log4j = LogManager.getLogger("QueueLedger");

	ChromeHeadless chrome = null;
	WordGenerator generator = null;
	String ChromeHeadlessConfigFile = null;

	String workerID = null;
	public static int exceedIndicator = 8; // If this number is achieved, TDK list is potentially longer
	private static int workerId = 0;

	public ChromeWorker(String cfgFileName) {
		super(cfgFileName);
		ChromeHeadlessConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		workerID = Integer.toString(getWorkerId());
		chrome = new ChromeHeadless(ChromeHeadlessConfigFile);
	}

	public static synchronized int getWorkerId() {
		int retval = workerId;
		workerId++;
		return retval;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		startProcess();
		chrome.closeChromeHeadless();
	}

	public void scrapeTDKAttributesFromWords() {
		try {
			log4j.info("psSelect1: " + psSelect1 + ", workerID:" + workerID + ", selectString1:" + selectString1);
			psSelect1.setString(1, workerID);
			ResultSet res = psSelect1.executeQuery();
			while (res.next()) {
				String word = res.getString("WORD");
				log4j.info(String.format("WORD[%s] WORKER_ID[%s]", word, this.workerID));
				TDKWordWrapper content = null; //chrome.scrapeTDKContentForWord(word);
				// log4j.info(content);

				for (TDKChapterItem chItem : content.getChapterItems()) {
					for (TDKMeaningItem meaning : chItem.getMeaningItems()) {
						log4j.info(String.format("%s, %s, %s, %s, %s", meaning.getChapterName(), meaning.getWordType(),
								meaning.getWord(), meaning.getChapterId(), meaning.getMeaningId()));
						psUpdate1.setString(1, meaning.getChapterName());
						psUpdate1.setString(2, meaning.getWordType());
						psUpdate1.setString(3, meaning.getWord());
						psUpdate1.setString(4, meaning.getChapterId());
						psUpdate1.setString(5, meaning.getMeaningId());
						int updated = psUpdate1.executeUpdate();
						log4j.info(String.format("%s meaning records updated ..", updated));
					}

				}

				psUpdate2.setString(1, word);
				psUpdate2.executeUpdate();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void scrapeTDKWordsWithKey() { // collect TDK keys over the list shown over the search editbox
		while (true) {
			try {
				psSelect1.setString(1, workerID);
				ResultSet res = psSelect1.executeQuery();
				while (res.next()) {
					String id = res.getString("ID");
					String word = res.getString("WORD");
					ArrayList<String> collect = null;
					try {
						collect = chrome.scrapeTDKWordsWithKey(word);
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					for (Iterator iterator = collect.iterator(); iterator.hasNext();) {
						String tmp1 = (String) iterator.next();
						psInsert1.setString(1, tmp1);
						psInsert1.setString(2, word);

						java.util.Date time = new java.util.Date();

						psInsert1.setString(3, format.format(time));
						psInsert1.setString(4, workerID);
						try {
							psInsert1.executeUpdate();
						} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
							log4j.info(String.format("%s is already in the TDK_SCRAPE table", tmp1));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					if (collect.size() >= exceedIndicator) {
						WordGenerator wordgen = new WordGenerator(word.length() + 1);
						Integer[] startArray = WordGenerator.getFirstArrayOfKey(word, 1);
						Integer[] endArray = WordGenerator.getLastArrayOfKey(word, 1);
						String startStr = WordGenerator.convertArrayToString(startArray);
						String endStr = WordGenerator.convertArrayToString(endArray);
						String cursor = startStr;
						wordgen.setStartValue(startStr);
						wordgen.setEndValue(endStr);
						do {
							psSelect2.setString(1, cursor); // check if this key is already in the ledger and run it if
															// it is not already looked up
							ResultSet resExists = psSelect2.executeQuery();
							if (resExists.next()) {
								log4j.info(String.format("Key %s already exists in the Database Ledger.. Skipping !!",
										cursor));
								continue;
							}

							if (cursor.contains("  ")) { // double space at the end is useless, dont add it to the queue
								continue;
							} else {
								addToQueueTable(cursor, word);
							}
						} while ((cursor = wordgen.nextWord()) != null);
					}
					psUpdate1.setString(1, id);
					psUpdate1.executeUpdate();
				}

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // Wait before you pull the next lot
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// add the newly discovered item (through TDK editbox) to the database to be
	// processed.
	public void addToQueueTable(String item, String source) {
		try {
			psInsert2.setString(1, item);
			psInsert2.setString(2, source);

			java.util.Date time = new java.util.Date();

			psInsert2.setString(3, format.format(time));
			psInsert2.setString(4, workerID);
			psInsert2.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			log4j.info(String.format("%s is already in the QUEUE table", item));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int Q_SIZE = 10;

		ArrayList<ChromeWorker> workerArray = new ArrayList<>();
		for (Integer i = 0; i < Q_SIZE; i++) {
			ChromeWorker queWorker = new ChromeWorker(args[0]);
			workerArray.add(queWorker);
			Thread t1 = new Thread(queWorker);
			t1.start();
		}
		System.out.println("classpath=" + System.getProperty("java.class.path"));

	}
}
