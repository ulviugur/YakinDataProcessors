package com.langpack.integration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.BasicClass;
import com.langpack.common.GlobalUtils;
import com.langpack.scraper.TurkishCharMapper;
import com.langpack.structure.SyllableBreaker;

// Read diff records from the database, scrape them and insert into another table
public class ProcessTable extends BasicClass {
	public static final Logger log4j = LogManager.getLogger("AnalyzeLedgerTable1");

	String TurkishCharsMappingFile = null;
	String TDKScrapeConfigFileName = null;
	TurkishCharMapper charMapper = null;

	TreeSet<String> wordList = new TreeSet<>();

	TDKScraper scraper = null;

	public ProcessTable(String cfgFileName) {
		super(cfgFileName);

		TurkishCharsMappingFile = cfg.getValue("TurkishCharMappingFile");
		charMapper = new TurkishCharMapper(TurkishCharsMappingFile);

		TDKScrapeConfigFileName = cfg.getValue("TDKScrapeConfigFile");
	}

	public void scanTablesforWords() {

		// extract all words in a given table using the given parser for level2 (both
		// defined by the configuration file)
		// add them all to #wordList#
		ResultSet records = null;

		int recordCount = 0;

		String realQueryString = null;
		try {
			realQueryString = selectString1;

			psSelect1 = DBconn.prepareStatement(realQueryString);
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			while (records.next()) {
				recordCount++;
				String word = records.getString(1);
				String id = records.getString(2);

				log4j.info(String.format("[id=%s] %s is processed ..", id, word));

				ArrayList<String> syls = null;
				String sylsString = null;

				try {
					syls = SyllableBreaker.break2Syls(word);
					if (syls.size() > 0) {
						sylsString = GlobalUtils.convertArrayToString(syls, ".");
						sylsString = sylsString.replace("._.", " ");
						psUpdate1.setString(1, sylsString);
						psUpdate1.setString(2, word);
						psUpdate1.setString(3, id);
						psUpdate1.executeUpdate();
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				recordCount++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Processed records : " + recordCount);

	}

	public void saveWordListIntoLedgerTable(TreeSet<String> importList) {
		int insertCount = 0;

		int fullCount = importList.size();

		for (String word : importList) {
			try {
				psInsert1.setString(1, word);
				psInsert1.setString(2, word);
				psInsert1.executeUpdate();

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			insertCount++;
			log4j.info(String.format("[%s/%s] Processed word : %s", insertCount, fullCount, word));
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ProcessTable instance = new ProcessTable(args[0]);
		instance.scanTablesforWords();
	}
}
