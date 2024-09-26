package com.langpack.integration;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.BasicClass;
import com.langpack.common.FileLedger;
import com.langpack.common.GlobalUtils;
import com.langpack.scraper.DictWord;
import com.langpack.scraper.TurkishCharMapper;

// Read diff records from the database, scrape them and insert into another table
public class AnalyzeTDKTable extends BasicClass {
	public static final Logger log4j = LogManager.getLogger("AnalyzeLedgerTable1");

	String TurkishCharsMappingFile = null;
	String TDKScrapeConfigFileName = null;
	TurkishCharMapper charMapper = null;

	String ruleSkipRestAfterNotFoundHNO = null;
	TreeSet<String> wordList = new TreeSet<>();

	TDKScraper scraper = null;
	TDKLevel2Parser level2Parser = new TDKLevel2Parser();
	FileLedger ledger = null;

	String ledgerFileName = null;

	public AnalyzeTDKTable(String cfgFileName) {
		super(cfgFileName);

		TurkishCharsMappingFile = cfg.getValue("TurkishCharMappingFile");
		charMapper = new TurkishCharMapper(TurkishCharsMappingFile);

		TDKScrapeConfigFileName = cfg.getValue("TDKScrapeConfigFile");
		scraper = new TDKScraper(TDKScrapeConfigFileName);

		ledgerFileName = cfg.getValue("ScrapeLedgerFile");

		try {
			ledger = new FileLedger(ledgerFileName);
			ledger.loadLedger();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Scrape content loaded from a list of words read from a table
	public void scrapeDataContent() {

		ResultSet records = null;
		try {
			log4j.info("Running : " + selectString1);
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int recordCount = 0;
		DictWord result = null;
		try {
			while (records.next()) {
				recordCount++;
				String id = records.getString("ID");
				String word = records.getString("WORD");

				result = scraper.lookupWordinTDKAndInsert(word, "TDK_SCRAPER", false);
				log4j.info(String.format("[%s]=> %s[%s]", recordCount, word, result.toString()));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void analyzeLevel2() {

		ResultSet records = null;
		int recordCount = 0;

		try {
			log4j.info("Running : " + selectString2);
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			while (records.next()) {
				recordCount++;
				String id = records.getString("ID");
				String refWord = records.getString("WORD");
				String level2 = records.getString("LEVEL2");

				log4j.info(String.format("[id=%s] %s is processed ..", id, refWord));
				TDKWordWrapper wrapper = level2Parser.parseJSONContent(level2);

				ArrayList<TDKChapterItem> chapterItems = wrapper.getChapterItems();

				for (TDKChapterItem chapterObject : chapterItems) {
					log4j.info(String.format("Processing chapter : %s [%s]", chapterObject.getChapterId(),
							chapterObject.getChapterName()));

					ArrayList<TDKMeaningItem> meaningItems = chapterObject.getMeaningItems();

					try {
						for (TDKMeaningItem item : meaningItems) {
							psInsert2.setString(1, refWord);
							psInsert2.setString(2, item.getChapterId());
							psInsert2.setString(3, item.getWordType());
							psInsert2.setString(4, item.getMeaningId());
							psInsert2.setString(5, item.getMeaning());
							psInsert2.setString(6, item.getLangCode());
							psInsert2.setString(7, item.getLangContent());
							psInsert2.setString(8, GlobalUtils.getCurrentDateString());
							psInsert2.executeUpdate();

							if (item.getSample() != null) { // add samples
								psInsert3.setString(1, refWord);
								psInsert3.setString(2, item.getChapterId());
								psInsert3.setString(3, item.getWordType());
								psInsert3.setString(4, item.getMeaningId());
								psInsert3.setString(5, item.getSample());
								psInsert3.setString(6, item.getSampleAuthor());
								psInsert3.setString(7, GlobalUtils.getCurrentDateString());
								psInsert3.executeUpdate();
							}
						}

						// insert into dict
						psInsert1.setString(1, refWord);
						psInsert1.setString(2, chapterObject.getChapterId());
						psInsert1.setString(3, chapterObject.getChapterName());
						psInsert1.setString(4, GlobalUtils.getCurrentDateString());
						psInsert1.executeUpdate();

						// mark as processed
						psUpdate1.setString(1, id);
						psUpdate1.executeUpdate();
						log4j.info(String.format("[%s] -> %s is processed successsfully", recordCount,
								chapterObject.getChapterName()));

					} catch (SQLException e) {
						// TODO Auto-generated catch block
						ledger.addtoLedger(String.format("%s > %s", refWord, e.getMessage()));
						log4j.info(String.format("[%s] -> %s had problems, checkLedgerFile : %s", recordCount, refWord,
								ledgerFileName));
					}

				}

				// Combinations are based on chapters
				TreeSet<String> combinations = wrapper.getCombinations();
				if (combinations != null) { // add combinations
					for (String item : combinations) {
						String[] tmp = item.split("-");
						psInsert4.setString(1, refWord);
						psInsert4.setString(2, tmp[0]);
						psInsert4.setString(3, tmp[1]);
						psInsert4.setString(4, "COMBI");
						psInsert4.setString(5, GlobalUtils.getCurrentDateString());
						psInsert4.executeUpdate();
					}
				}

				// proverbs are based on chapters
				TreeSet<String> proverbs = wrapper.getProverbs();
				if (proverbs != null) { // add combinations
					for (String item : proverbs) {
						String[] tmp = item.split("-");
						psInsert4.setString(1, refWord);
						psInsert4.setString(2, tmp[0]);
						psInsert4.setString(3, tmp[1]);
						psInsert4.setString(4, "PHRASE");
						psInsert4.setString(5, GlobalUtils.getCurrentDateString());
						psInsert4.executeUpdate();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log4j.info("Processed records : " + recordCount);
		ledger.closeLedger();
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		AnalyzeTDKTable instance = new AnalyzeTDKTable(args[0]);
		// instance.scrapeDataContent();
		instance.startProcess();
	}
}
