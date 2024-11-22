package com.langpack.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.scraper.PageParser;
import com.langpack.scraper.Qualifier;
import com.langpack.scraper.SimpleHTMLTag;

// Read diff records from the database, scrape them and insert into another table
public class ZLibraryDownloader {
	public static final Logger logger = LogManager.getLogger("ZLibraryDownloader");
	private String errorPageIndicator = null;
	private String baseURL = null;

	static String dbDriver = null;
	static String dbURL = null;
	static String dbUser = null;
	static String dbPass = null;

	String insertString1 = null;
	String selectString1 = null;
	String updateString1 = null;
	String updateString2 = null;
	String ledgerManagerConfig = null;

	String downloadFolderName = null;
	LedgerManager ledgerManager = null;

	Connection DBconn = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	PreparedStatement psSelect1 = null;
	PreparedStatement psInsert1 = null;
	PreparedStatement psUpdate1 = null;
	PreparedStatement psUpdate2 = null;

	ConfigReader cfgObject = null;
	String VALUES_SEPARATOR = ";";

	public ZLibraryDownloader(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		HashMap<String, String> map = cfgObject.getParameterMap();
		// ledgerManagerConfig = cfgObject.getValue("LedgerManagerConfig");
		// ledgerManager = new LedgerManager(ledgerManagerConfig);

		errorPageIndicator = cfgObject.getValue("Download.ErrorPageIndicator");
		baseURL = cfgObject.getValue("Download.Site");
		downloadFolderName = cfgObject.getValue("Export.Folder");

		dbDriver = cfgObject.getValue("db1.Driver");
		dbURL = cfgObject.getValue("db1.URL");
		dbUser = cfgObject.getValue("db1.User");
		dbPass = cfgObject.getValue("db1.Password");

		selectString1 = cfgObject.getValue("db1.SelectQuery1");
		insertString1 = cfgObject.getValue("db1.InsertQuery1");
		updateString1 = cfgObject.getValue("db1.UpdateQuery1");
		updateString2 = cfgObject.getValue("db1.UpdateQuery2");

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
			psSelect1 = DBconn.prepareStatement(selectString1);
			psUpdate1 = DBconn.prepareStatement(updateString1);
			psUpdate2 = DBconn.prepareStatement(updateString2);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		logger.info("DB connections established ..");
	}

	public void processLevel1() {
		// All Turkish URLs are downloaded
		String[] EXTENSION_ARRAY = new String[] { "pdf", "epub", "djvu", "fb2", "txt", "rar", "mobi", "lit", "doc",
				"rtf", "azw3" };

		for (String extension : EXTENSION_ARRAY) {
			int START_YEAR = 1800;
			int END_YEAR = 2020;

			String subFormat = baseURL + "/s/?yearFrom=%s&yearTo=%s&languages_FILLER_=turkish&extension_FILLER_=%s";

			int currentYear = START_YEAR;
			for (; currentYear < END_YEAR; currentYear++) {

				// String getURL = String.format(subFormat, currentYear, END_YEAR, EXTENSION);
				String getURL0 = String.format(subFormat, currentYear, currentYear + 1, extension);
				String getURL = getURL0.replace("_FILLER_", "%5B%5D");

				logger.info(getURL);

				try {

					PageParser parser = new PageParser();
					String pageContent = GlobalUtils.callWebsite(getURL, 10000);

					int page = 1;
					String pageURL = null;

					while (page > -1) {

						int count = 0;
						while (count > -1) {

							Qualifier qualifier1 = new Qualifier("<h3 itemprop=\"name\">", count, "<a ", "</a>",
									Qualifier.GO_FORWARD);
							String block1 = parser.findBlock(pageContent, qualifier1);

							// logger.info("+-+++++++++++++++++++++++++++ END " + result);
							if (block1 == null) {
								// logger.info("+-+++++++++++++++++++++++++++ END " + result);
								count = -1;
							} else {
								SimpleHTMLTag htmlTag = new SimpleHTMLTag(block1, true);
								String bookName = htmlTag.getContent();
								String subURL = htmlTag.getLink();
								String fullURL = baseURL + subURL;

								psInsert1.setString(1, fullURL);
								psInsert1.setString(2, baseURL);
								psInsert1.setString(3, subURL);
								psInsert1.setString(4, bookName);
								psInsert1.setString(5, extension);

								psInsert1.executeUpdate();

								// logger.info(String.format("[ %s | %s | %s ]", content, link, attr));
								count++;
							}
						}

						page++;

						Qualifier qualifier1 = new Qualifier("page=" + page, 0, "<a ", "</a>", Qualifier.GO_BACKWARD);
						String pageBlock = parser.findBlock(pageContent, qualifier1);

						if (pageBlock == null) {
							page = -1;
						} else {
							SimpleHTMLTag htmlTag = new SimpleHTMLTag(pageBlock, true);
							String subURL = htmlTag.getLink();
							pageURL = baseURL + subURL;
							logger.info(subURL);
							logger.info("");
							Thread.sleep(500);
							pageContent = GlobalUtils.callWebsite(pageURL, 10000);
						}
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public void runTable() {
		int count = 0;
		String id = null;
		String baseURL = null;
		String fullURL = null;
		String contentType = null;
		File contentFile = null;
		ResultSet records = null;
		String downloaded = "N";

		try {
			logger.info(String.format("Loading booklist from reference table .."));
			records = psSelect1.executeQuery();
			while (records.next()) {
				id = records.getString("ID");
				// baseURL = records.getString("BASEURL");
				fullURL = records.getString("FULLURL");
				contentType = records.getString("CONTENTTYPE");

				try {
					Thread.sleep(1000);
					downloadBookDetails(id, fullURL, baseURL);
					Thread.sleep(25000);
//					contentFile = downloadFile(fullURL, baseURL);
//					downloaded="Y";
//					psUpdate1.setString(3, contentFile.getAbsolutePath());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
//					downloaded = e1.getMessage();
//					psUpdate1.setString(3, null);
				}

				try {

//					java.util.Date time = new java.util.Date();
//					psUpdate1.setString(1, downloaded);
//					psUpdate1.setString(2, format.format(time));
//					psUpdate1.setString(4, id);

//					psUpdate1.executeUpdate();
					count++;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void downloadBookDetails(String id, String downloadPageURL, String baseURL) {

		logger.info(String.format("Processing %s, from website':%s", id, downloadPageURL));

		Response res1 = null;
		Document doc = null;

		try {
			res1 = Jsoup.connect(downloadPageURL).execute();

			doc = res1.parse();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String authorsString = null;
		String bookSynopsis = null;
		byte[] synopsisBytes = null;
		String categoriesString = null;
		String yearString = null;
		String editionString = null;
		String publisherString = null;
		String languageString = null;
		String noPagesString = null;
		String seriesString = null;
		String fileAttrString = null;

		Element content = doc.body();

		Element nodeAttributes = content.getElementsByAttributeValue("class", "col-sm-9").first();
		String bookName = nodeAttributes.getElementsByAttributeValue("itemprop", "name").first().text();

		logger.info(String.format("Processing book : %s", bookName));

		Element divCoverPicture = content.getElementsByAttributeValue("class", "col-sm-3 details-book-cover-container")
				.first();
		Element nodeCoverURL = divCoverPicture.child(0);
		String coverURL = nodeCoverURL.attr("href");

		Element nodeDownloadURL = content
				.getElementsByAttributeValue("class", "btn btn-primary dlButton addDownloadedBook").first();
		String downloadURL = baseURL + nodeDownloadURL.attr("href");

		Elements authorAttributes = nodeAttributes.getElementsByAttributeValue("itemprop", "author");
		authorsString = getAttributeValuesAsString(authorAttributes, "itemprop", "author");

		Elements nodeBookSynopsis = nodeAttributes.getElementsByAttributeValue("itemprop", "reviewBody");
		if (nodeBookSynopsis != null && nodeBookSynopsis.first() != null) {
			bookSynopsis = nodeBookSynopsis.first().text();
			if (bookSynopsis != null) {
				synopsisBytes = bookSynopsis.getBytes(UTF8_CHARSET);
			}
		}

		Element nodeDetails = nodeAttributes.getElementsByAttributeValue("class", "bookDetailsBox").first();
		Elements nodeCategory = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_categories");
		categoriesString = getAttributeValuesAsString(nodeCategory, "class", "property_value");

		Element nodeYear = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_year").first();
		yearString = getAttributeValuesAsString(nodeYear, "class", "property_value");

		Element nodeEdition = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_edition").first();
		if (nodeEdition != null) {
			editionString = getAttributeValuesAsString(nodeEdition, "class", "property_value");
		}

		Element nodePublisher = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_publisher")
				.first();
		publisherString = getAttributeValuesAsString(nodePublisher, "class", "property_value");

		Element nodeLanguage = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_language")
				.first();
		languageString = getAttributeValuesAsString(nodeLanguage, "class", "property_value");

		Element nodePages = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_pages").first();
		noPagesString = getAttributeValuesAsString(nodePages, "class", "property_value");

		Element nodeSeries = nodeDetails.getElementsByAttributeValue("class", "bookProperty property_series").first();
		if (nodeSeries != null) {
			seriesString = getAttributeValuesAsString(nodeSeries, "class", "property_value");
		}

		Element nodeFileAttr = nodeDetails.getElementsByAttributeValue("class", "bookProperty property__file").first();
		fileAttrString = getAttributeValuesAsString(nodeFileAttr, "class", "property_value");

		byte[] pict = downloadBytes(coverURL);

		try {
			psUpdate1.setString(1, downloadURL);
			psUpdate1.setString(2, authorsString);
			psUpdate1.setBytes(3, pict);
			psUpdate1.setString(4, coverURL);
			psUpdate1.setBytes(5, synopsisBytes);
			psUpdate1.setString(6, categoriesString);
			psUpdate1.setString(7, yearString);
			psUpdate1.setString(8, editionString);
			psUpdate1.setString(9, publisherString);
			psUpdate1.setString(10, languageString);
			psUpdate1.setString(11, noPagesString);
			psUpdate1.setString(12, fileAttrString);
			psUpdate1.setString(13, id);
			int updated = psUpdate1.executeUpdate();
			logger.info(String.format("Updated %s records in books table", updated));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public byte[] downloadBytes(String URL) {
		byte[] bytes = null;

		Response res2 = null;
		try {
			res2 = Jsoup.connect(URL).header("Accept-Encoding", "epub")
					.header("Content-Type", "application/pdf, text/*, application/xhtml+xml, ")
					.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
					.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1").maxBodySize(0)
					.timeout(600000).ignoreContentType(true)
					.userAgent(
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
					.execute();

			Map<String, String> headers = res2.headers();
			/*
			 * String disp = headers.get("Content-Disposition"); String[] params =
			 * disp.split(";"); String fileName = params[1].replaceAll("^\\s+",
			 * "").replaceAll("^filename=", "").replace("\"", "").replaceAll("\\:",
			 * "").replaceAll("\\*", ""); fileName = fileName.replaceAll("\\|",
			 * "").replaceAll("\\*", "").replaceAll("\\<", "\\(").replaceAll("\\>",
			 * "\\)").replaceAll("\\?", ""); fileName = fileName.replaceAll("\\,",
			 * "").replaceAll("\\;", "").replaceAll("\\s+", "\\_");
			 */

			bytes = res2.bodyAsBytes();
		} catch (Exception ex) {

		}

		/*
		 * File outFile = new File(new File(downloadFolderName), "testFile.jpg");
		 * FileOutputStream os = null; try { os = new FileOutputStream(outFile);
		 * os.write(bytes); os.close(); } catch (IOException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 */

		return bytes;
	}

	public File downloadFile(String downloadPageURL, String baseURL) throws Exception {
		File outFile = null;
		// String URL =
		// "https://file-examples-com.github.io/uploads/2017/10/file-sample_150kB.pdf";
		String furtherMIMETypes = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types2";

		Response res1 = null;
		Document doc = null;

		try {
			res1 = Jsoup.connect(downloadPageURL).header("Accept-Encoding", "epub")
					.header("Content-Type", "application/pdf, text/*, application/xhtml+xml, ")
					.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
					.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1").maxBodySize(0)
					.timeout(600000).ignoreContentType(true)
					.userAgent(
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
					.execute();

			doc = res1.parse();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Element content = doc.body();
		String downloadURL = null;
		if (content != null) {
			PageParser parser2 = new PageParser();
			String text = content.toString();
			logger.info("***" + text + "***");
			Qualifier itemQualifier = new Qualifier("addDownloadedBook", 0, "<a", "/a>", Qualifier.GO_BACKWARD);
			String blockItem = parser2.findBlock(text, itemQualifier);
			SimpleHTMLTag tag = new SimpleHTMLTag(blockItem);
			String linkURL = tag.getAttr("href");
			downloadURL = baseURL + linkURL;
		}
		logger.info("");
		Response res2 = null;
		try {
			res2 = Jsoup.connect(downloadURL).header("Accept-Encoding", "epub")
					.header("Content-Type", "application/pdf, text/*, application/xhtml+xml, ")
					.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
					.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1").maxBodySize(0)
					.timeout(600000).ignoreContentType(true)
					.userAgent(
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
					.execute();

			Map<String, String> headers = res2.headers();
			String disp = headers.get("Content-Disposition");
			String[] params = disp.split(";");
			String fileName = params[1].replaceAll("^\\s+", "").replaceAll("^filename=", "").replace("\"", "")
					.replaceAll("\\:", "").replaceAll("\\*", "");
			fileName = fileName.replaceAll("\\|", "").replaceAll("\\*", "").replaceAll("\\<", "\\(")
					.replaceAll("\\>", "\\)").replaceAll("\\?", "");
			fileName = fileName.replaceAll("\\,", "").replaceAll("\\;", "").replaceAll("\\s+", "\\_");

			byte[] bytes = res2.bodyAsBytes();

			outFile = new File(new File(downloadFolderName), fileName);
			FileOutputStream os = new FileOutputStream(outFile);
			os.write(bytes);
			os.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		}
		return outFile;
	}

	public void testSocksProxy(String URL) {
		Response res1 = null;
		Document doc = null;
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("208.102.51.6", 58208));
		try {
			/*
			 * doc = Jsoup.connect(URL) .header("Accept-Encoding", "epub")
			 * .header("Content-Type", "application/pdf, text/*, application/xhtml+xml, ")
			 * .header("Accept-Language", "en-US,en;q=0.9,de;q=0.8")
			 * .header("Cache-Control", "max-age=0") .header("Connection", "keep-alive")
			 * .header("Upgrade-Insecure-Requests", "1") .maxBodySize(0) .timeout(600000)
			 * .ignoreContentType(true) .proxy("208.102.51.6", 58208)
			 * .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
			 * ) .get();
			 */

			try {
				res1 = Jsoup.connect(URL).header("Accept-Encoding", "epub")
						.header("Content-Type", "application/pdf, text/*, application/xhtml+xml, ")
						.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
						.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1").maxBodySize(0)
						.timeout(600000).ignoreContentType(true).proxy(proxy)
						.userAgent(
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
						.execute();

				doc = res1.parse();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Element content = doc.body();
			String text = content.toString();
			logger.info("Webpage : " + text);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private ArrayList<String> getAttributeValuesAsArray(Element node, String attrName, String attrValue) {
		ArrayList<String> retval = new ArrayList<>();
		if (node != null) {
			Elements nodeValues = node.getElementsByAttributeValue(attrName, attrValue);
			for (Iterator<Element> iterator = nodeValues.iterator(); iterator.hasNext();) {
				Element element = iterator.next();
				retval.add(element.text());
			}
		}
		return retval;
	}

	private ArrayList<String> getAttributeValuesAsArray(Elements nodes, String attrName, String attrValue) {
		ArrayList<String> retval = new ArrayList<>();
		if (nodes != null) {
			for (Iterator<Element> iterator = nodes.iterator(); iterator.hasNext();) {
				Element element = iterator.next();
				retval.add(element.text());
			}
		}
		return retval;
	}

	private String getAttributeValuesAsString(Elements nodes, String attrName, String attrValue) {
		String retval = GlobalUtils.convertArrayToString(getAttributeValuesAsArray(nodes, attrName, attrValue),
				VALUES_SEPARATOR);
		return retval;
	}

	private String getAttributeValuesAsString(Element node, String attrName, String attrValue) {
		String retval = GlobalUtils.convertArrayToString(getAttributeValuesAsArray(node, attrName, attrValue),
				VALUES_SEPARATOR);
		return retval;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ZLibraryDownloader instance = new ZLibraryDownloader(args[0]);
		instance.runTable();
		// instance.processLevel1();
		// instance.processEpubFiles();
		// String fullURL =
		// "https://www.pdfdrive.com/download.pdf?id=157165730&h=ec4f76fef81347295b22cee384353cb0&u=cache&ext=pdf";
		// String fullURL = "file://S:/Ulvi/wordspace/Wordlet/_BOOKS/pdf/Yatırımcı
		// Sözlüğü by Kolektif (z-lib.org).pdf";
		// instance.downloadFile(fullURL);

		try {
			// instance.downloadBookDetails("2", "https://b-ok.cc/book/2780007/c083f7",
			// "https://b-ok.cc");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// instance.testSocksProxy("https://whatismyipaddress.com/");

		// instance.testSocksProxy("https://b-ok.cc/book/2576691/c30090");
		// instance.runTable();
		// byte[] res =
		// instance.downloadBytes("https://covers.zlibcdn2.com/covers/books/be/bc/79/bebc790f747989d7ffa542934461f75d.jpg");

	}
}
