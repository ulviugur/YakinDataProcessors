package com.langpack.dbprocess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.integration.TDKScraper;
import com.langpack.scraper.DictWord;
import com.langpack.scraper.HtmlTagList;
import com.langpack.scraper.TurkishCharMapper;

// Read diff records from the database, scrape them and insert into another table
public class LedgerAnalyser {
	public static final Logger log4j = LogManager.getLogger("LedgerAnalyser");

	static String dbDriver = null;
	static String dbURL = null;
	static String dbUser = null;
	static String dbPass = null;

	String insertString1 = null;
	String selectString1 = null;
	String selectString2 = null;
	String updateString1 = null;
	String updateString2 = null;

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

	String ruleSkipRestAfterNotFoundHNO = null;
	TreeSet<String> wordList = new TreeSet<>();

	Connection DBconn = null;
	TDKScraper scraper = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement psSelect1 = null;
	PreparedStatement psSelect2 = null;
	PreparedStatement psInsert = null;
	PreparedStatement psUpdate1 = null;
	PreparedStatement psUpdate2 = null;

	ConfigReader cfgObject = null;

	public LedgerAnalyser(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		TDKScrapeConfigFileName = cfgObject.getValue("TDKScrapeConfigFile");

		dbDriver = cfgObject.getValue("db1.Driver");
		dbURL = cfgObject.getValue("db1.URL");
		dbUser = cfgObject.getValue("db1.User");
		dbPass = cfgObject.getValue("db1.Password");

		selectString1 = cfgObject.getValue("db1.SelectQuery1");
		/*
		 * selectString2 = cfgObject.getValue("db1.SelectQuery2");
		 *
		 * insertString1 = cfgObject.getValue("db1.InsertQuery1"); updateString1 =
		 * cfgObject.getValue("db1.UpdateQuery1"); updateString2 =
		 * cfgObject.getValue("db1.UpdateQuery2");
		 */

		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			log4j.info("Establishing connection with the Database ..");
			DBconn = DriverManager.getConnection(dbURL, dbUser, dbPass);
			DBconn.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {

			scraper = new TDKScraper(TDKScrapeConfigFileName);

			psSelect1 = DBconn.prepareStatement(selectString1);

			/*
			 * psInsert = DBconn.prepareStatement(insertString1); psSelect2 =
			 * DBconn.prepareStatement(selectString2); psUpdate1 =
			 * DBconn.prepareStatement(updateString1); psUpdate2 =
			 * DBconn.prepareStatement(updateString2);
			 */

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		log4j.info("DB connections established ..");
	}

	public void scanTablesforWords() {

		// extract all words in a given table using the given parser for level2 (both
		// defined by the configuration file)
		// add them all to #wordList#
		ResultSet records = null;

		int recordCount = 0;

		for (int tableCount = 0; tableCount < parserList.length; tableCount++) {
			String tableName = tableList[tableCount];

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
			records = psSelect1.executeQuery();
			while (records.next()) {
				String id = records.getString("ID");
				String word = records.getString("WORD");
				String source = records.getString("SOURCE");
				// log4j.info(String.format("%s -> %s", id, word));

				DictWord wordObject = scraper.lookupWordinTDKAndInsert(word, source, false);

				if (wordObject.isFound()) {
					log4j.info(String.format("%s -> %s", id, word));
				}

				recordCount++;
				// logger.info(String.format("[%s] Updated record id: %s, %s: ", recordCount,
				// id, word));

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		LedgerAnalyser instance = new LedgerAnalyser(args[0]);
		instance.lookupInTDK();

		// instance.scanTablesforWords();
		// instance.saveWordListingDB();

	}

}
