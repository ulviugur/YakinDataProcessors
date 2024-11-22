package com.langpack.integration;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.scraper.PageParser;
import com.langpack.scraper.Qualifier;
import com.langpack.scraper.SimpleHTMLTag;

// Read diff records from the database, scrape them and insert into another table
public class DDScrapeAndInsert {
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
	String insertString2 = null;
	String selectString1 = null;
	String updateString1 = null;

	String ruleSkipRestAfterNotFoundHNO = null;

	Connection DBconn = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement psSelect = null;
	PreparedStatement psInsert1 = null;
	PreparedStatement psInsert2 = null;
	PreparedStatement psUpdate = null;

	ConfigReader cfgObject = null;

	public DDScrapeAndInsert(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		errorPageIndicator = cfgObject.getValue("Download.ErrorPageIndicator");
		sourceFolder = cfgObject.getValue("Download.Site");
		exportFolder = cfgObject.getValue("Export.Folder");

		dbDriver = cfgObject.getValue("db1.Driver");
		dbURL = cfgObject.getValue("db1.URL");
		dbUser = cfgObject.getValue("db1.User");
		dbPass = cfgObject.getValue("db1.Password");

		selectString1 = cfgObject.getValue("db1.SelectQuery");
		insertString1 = cfgObject.getValue("db1.InsertQuery1");
		insertString2 = cfgObject.getValue("db1.InsertQuery2");
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
			psInsert1 = DBconn.prepareStatement(insertString1);
			psInsert2 = DBconn.prepareStatement(insertString2);
			psSelect = DBconn.prepareStatement(selectString1);
			psUpdate = DBconn.prepareStatement(updateString1);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		logger.info("DB connections established ..");
	}

	public void processLevel2() {
		String fullURL = null;
		ResultSet records = null;

		int processCount = 0;

		try {
			records = psSelect.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (records.next()) {
				String id = records.getString(1);
				String word = records.getString(2);
				Response res = null;
				Document doc = null;

				fullURL = String.format("%s%s", sourceFolder, word);

				try {
					Thread.sleep(300);
					doc = Jsoup.connect(fullURL).get();

				} catch (InterruptedException e1) {
				} catch (org.jsoup.HttpStatusException e1) {
					e1.printStackTrace();
					logger.info(String.format("Error in getting [%s] : %s, trying again", processCount, word));
					try {
						Thread.sleep(300);
						doc = Jsoup.connect(fullURL).get();

					} catch (InterruptedException e2) {

					} catch (org.jsoup.HttpStatusException e2) {
						logger.info(String.format("Error in getting [%s] : %s, giving up", processCount, word));
						continue;
					}

				}

				Element body = doc.body();
				String content = body.toString();

				boolean errorOccurance = doc.html().contains(errorPageIndicator);

				if (errorOccurance) {
					content = String.format("%s does not exist in %s", word, sourceFolder);
					logger.info(String.format("No meaning found for [%s] : %s", processCount, word));

				} else {
					logger.info(String.format("Meanings found for [%s] : %s", processCount, word));
					//
					parser.setPageContent(content);
					int meaningCount = 1;

					while (true) {

						String key = String.format(
								"<td class=\"footable-first-visible\" style=\"display: table-cell;\">%s</td>",
								meaningCount);
						Qualifier rowQualifier1 = new Qualifier(key, 0, "<tr>", "</tr>", Qualifier.GO_BACKWARD);

						String block1 = parser.findBlock(content, rowQualifier1);
						if (block1 != null) {

							ArrayList<String> values = new ArrayList<>();
							values.add(Integer.toString(meaningCount));

							for (int i = 0; i < 6; i++) {
								Qualifier fieldQualifier1 = new Qualifier("<td", i, "<td", "</td>",
										Qualifier.GO_FORWARD);
								String field = parser.findBlock(block1, fieldQualifier1);
								SimpleHTMLTag tag = new SimpleHTMLTag(field);
								tag.setSource(field);
								tag.parse();
								values.add(tag.getContent());
							}
							java.util.Date time = new java.util.Date();
							try {

								psInsert2.setString(1, word);
								psInsert2.setString(2, values.get(2));
								psInsert2.setString(3, fullURL);
								psInsert2.setString(4, values.get(0));
								psInsert2.setString(5, values.get(5));
								psInsert2.setString(6, values.get(1));
								psInsert2.setString(7, values.get(3));
								psInsert2.setString(8, values.get(4));
								psInsert2.setString(9, format.format(time));

								psInsert2.executeUpdate();
								// logger.debug("Inserted record : " + tagItem.toString());
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								System.out.println(GlobalUtils.convertArrayToString(values, ","));
							}

						} else {
							break;
						}
						meaningCount++;
					}
				}
				processCount++;
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

		DDScrapeAndInsert instance = new DDScrapeAndInsert(args[0]);
		instance.processLevel2();
	}

}
