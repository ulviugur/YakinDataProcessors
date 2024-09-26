package com.langpack.integration;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.langpack.common.ConfigReader;
import com.langpack.scraper.PageParser;

// Read diff records from the database, scrape them and insert into another table
public class ZarganaScrapeAndInsert {
	public static final Logger logger = LogManager.getLogger("ScrapeAndInsert");
	private String errorPageIndicator = null;
	private String sourceFolder = null;
	private String exportFolder = null;

	private PageParser parser = new PageParser();
	private FileWriter writer = null;

	static String dbDriver = null;
	static String dbURL = null;
	static String dbUser = null;
	static String dbPass = null;

	String insertString1 = null;
	String selectString1 = null;
	String updateString1 = null;

	String ruleSkipRestAfterNotFoundHNO = null;

	Connection DBconn = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement psSelect = null;
	PreparedStatement psInsert = null;
	PreparedStatement psUpdate = null;

	ConfigReader cfgObject = null;

	public ZarganaScrapeAndInsert(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		errorPageIndicator = cfgObject.getValue("Download.ErrorPageIndicator");
		sourceFolder = cfgObject.getValue("Download.Site");
		exportFolder = cfgObject.getValue("Export.Folder");

		dbDriver = cfgObject.getValue("db1.Driver");
		dbURL = cfgObject.getValue("db1.URL");
		dbUser = cfgObject.getValue("db1.User");
		dbPass = cfgObject.getValue("db1.Password");

		selectString1 = cfgObject.getValue("db1.SelectQuery");
		insertString1 = cfgObject.getValue("db1.InsertQuery");
		updateString1 = cfgObject.getValue("db1.UpdateQuery");

		ruleSkipRestAfterNotFoundHNO = cfgObject.getValue("Download.Rules.SkipRestAfterNotFoundHNO", "N");

		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			logger.info("Establishing connection with the Database ..");
			DBconn = DriverManager.getConnection(dbURL, dbUser, dbPass);
			DBconn.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			psInsert = DBconn.prepareStatement(insertString1);
			psSelect = DBconn.prepareStatement(selectString1);
			psUpdate = DBconn.prepareStatement(updateString1);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		logger.info("DB connections established ..");
	}

	private void removeComments(Node node) {
		for (int i = 0; i < node.childNodeSize();) {

			Node child = node.childNode(i);
			if ("#comment".equals(child.nodeName())) {
				// logger.info("Need to remove >>> " + child.toString());
				// logger.info("Before >>> " + node.toString());
				child.remove();
				// logger.info("After >>> " + node.toString());
			} else {
				removeComments(child);
				i++;
			}
		}
	}

	public void processLevel2() {
		String fullURL = null;
		boolean pageFound = false;
		ResultSet records = null;

		int count = 0;

		try {
			records = psSelect.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (records.next()) {
				String word = records.getString(1);
				String source = records.getString(2);
				String id = records.getString(3);

				fullURL = String.format("%s/tr/q/%s-ceviri-nedir", sourceFolder, word);

				Response res = null;
				Document doc = null;

				res = Jsoup.connect(fullURL).header("Accept-Encoding", "gzip, deflate, br")
						.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
						.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
						.userAgent(
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
						.execute();

				doc = res.parse();
				String errorString = String.format("\"%s\" terimi bulunamadı", word);
				boolean errorOccurance = doc.html().contains(errorString);

				Elements content = new Elements();
				String contentString = null;

				if (errorOccurance) {
					contentString = String.format("%s does not exist in zargan.com", word);
				} else {
					content = doc.select("div[id$=resultsContainer]");
					if (content == null) {
						contentString = String.format("%s does not exist in zargan.com", word);
					} else {
						contentString = content.toString();
					}
				}

				logger.info("+++++ Header found in " + fullURL);
				java.util.Date time = new java.util.Date();
				try {
					psInsert.setString(1, word);
					psInsert.setString(2, sourceFolder);
					psInsert.setString(3, fullURL);
					psInsert.setString(4, contentString);
					psInsert.setString(5, format.format(time));
					psInsert.executeUpdate();
					count++;
					// logger.debug("Inserted record : " + tagItem.toString());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
				count++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private void writeFile(String tmpFile, String content) {
		try {
			writer = new FileWriter(tmpFile);
			writer.write(content);
			writer.flush();
			writer.close();
			logger.debug("Completed writing file : " + tmpFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ZarganaScrapeAndInsert instance = new ZarganaScrapeAndInsert(args[0]);
		instance.processLevel2();
	}

}
