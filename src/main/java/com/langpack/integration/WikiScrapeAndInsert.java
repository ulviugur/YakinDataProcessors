package com.langpack.integration;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import com.langpack.common.ConfigReader;
import com.langpack.scraper.PageParser;
import com.langpack.scraper.Qualifier;
import com.langpack.scraper.SimpleHTMLTag;

// Read diff records from the database, scrape them and insert into another table
public class WikiScrapeAndInsert {
	public static final Logger logger = LogManager.getLogger("ScrapeAndInsert");
	private String errorPageIndicator = null;
	private String sourceFolder = null;
	private String exportFolder = null;

	private PageParser parser = null;
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

	public static String MW = "<div> \r\n" + " <p>عبدُ الحيّ </p> \r\n" + " <!-- \r\n" + "NewPP limit report\r\n"
			+ "Parsed by mw1334\r\n" + "Cached time: 20200512034450\r\n" + "Cache expiry: 2592000\r\n"
			+ "Dynamic content: false\r\n" + "Complications: []\r\n" + "CPU time usage: 0.260 seconds\r\n"
			+ "Real time usage: 0.377 seconds\r\n" + "Preprocessor visited node count: 67/1000000\r\n"
			+ "Post‐expand include size: 1185/2097152 bytes\r\n" + "Template argument size: 62/2097152 bytes\r\n"
			+ "Highest expansion depth: 10/40\r\n" + "Expensive parser function count: 0/500\r\n"
			+ "Unstrip recursion depth: 0/20\r\n" + "Unstrip post‐expand size: 0/5000000 bytes\r\n"
			+ "Number of Wikibase entities loaded: 0/400\r\n" + "Lua time usage: 0.224/10.000 seconds\r\n"
			+ "Lua memory usage: 10.76 MB/50 MB\r\n" + "--> \r\n" + " <!--\r\n"
			+ "Transclusion expansion time report (%,ms,calls,template)\r\n" + "100.00%  366.588      1 -total\r\n"
			+ " 88.44%  324.203      1 Şablon:tr-özel_ad\r\n" + "  8.38%   30.714      1 Şablon:köken\r\n"
			+ "  3.13%   11.483      1 Şablon:özel_ad\r\n" + "  1.42%    5.199      1 Şablon:Dil_kodu\r\n"
			+ "  0.80%    2.942      2 Şablon:özel_ad/liste\r\n" + "--> \r\n"
			+ " <!-- Saved in parser cache with key trwiktionary:pcache:idhash:182473-0!canonical and timestamp 20200512034450 and revision id 3277555\r\n"
			+ " --> \r\n" + "</div>";

	ConfigReader cfgObject = null;

	public WikiScrapeAndInsert(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		parser = new PageParser();

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

		try {

			psInsert = DBconn.prepareStatement(insertString1);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		fullURL = String.format("%s/w/index.php?title=Kategori:Türkçe_sözcükler&from=A", sourceFolder);

		String page = callWebsite(fullURL);
		if (page == null) {
			page = "";
			pageFound = false;
		} else {
			pageFound = true;
		}

		if (pageFound) {
			Qualifier pageQualifier = new Qualifier("kategorisindeki sayfalar", 0, "<ul>", "</ul>",
					Qualifier.GO_FORWARD);
			parser.setPageContent(page);
			String block = parser.findBlock(page, pageQualifier);
			if (block == null) {
				logger.error("Header not found !");
			} else {
				logger.info("+++++ Header found in " + fullURL);
				insertItemsFromBlock(block);
				// End of page, jump to the next page

				int count = 1;
				while (true) {

					Qualifier nextPageQualifier = new Qualifier("sonraki sayfa", 3, "<a", "</a>",
							Qualifier.GO_BACKWARD);
					String nextPageBlock = parser.findBlock(page, nextPageQualifier);

					logger.info(String.format("Processing loop : %s", count));
					if (nextPageBlock != null) {

						SimpleHTMLTag nextPageTag = new SimpleHTMLTag(nextPageBlock, true);
						String nextPageLink = nextPageTag.getAttr("href");
						fullURL = String.format("%s%s", sourceFolder, nextPageLink);
						page = callWebsite(fullURL);

						insertItemsFromBlock(page);

					} else {
						break;
					}
					count++;
				}
			}
		}
	}

	private void insertItemsFromBlock(String block) {
		logger.info(block);

		String word = null;
		String meaning = null;
		String wordtype = null;
		String lang1 = null;
		String lang2 = null;

		SimpleHTMLTag tag = new SimpleHTMLTag(block, true);

		int count = 0;
		PageParser parser2 = new PageParser();
		while (true) {
			parser2.setPageContent(tag.getContent());
			Qualifier itemQualifier = new Qualifier("<li>", count, "<a", "/a>", Qualifier.GO_FORWARD);
			String blockItem = parser2.findBlock(tag.getSource(), itemQualifier);

			if (blockItem == null) {
				logger.info("Came to the end of the list @ item " + count);
				break;
			}

			SimpleHTMLTag tagItem = new SimpleHTMLTag(blockItem, true);

			java.util.Date time = new java.util.Date();
			try {
				psInsert.setString(1, tagItem.getContent());
				psInsert.setString(2, sourceFolder);
				psInsert.setString(3, meaning);
				psInsert.setString(4, wordtype);
				psInsert.setString(5, lang1);
				psInsert.setString(6, lang2);
				psInsert.setString(7, tagItem.getLink());
				psInsert.setString(8, format.format(time));
				psInsert.executeUpdate();

				count++;
				// logger.debug("Inserted record : " + tagItem.toString());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
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
	/*
	 * public String readPage(String myURL) {
	 *
	 * System.out.println("Requested URL:" + myURL); StringBuilder sb = new
	 * StringBuilder(); URLConnection urlConn = null; InputStreamReader in = null;
	 * try { URL url = new URL(myURL); urlConn = url.openConnection(); if (urlConn
	 * != null) urlConn.setReadTimeout(60 * 1000); if (urlConn != null &&
	 * urlConn.getInputStream() != null) { in = new
	 * InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
	 * BufferedReader bufferedReader = new BufferedReader(in); if (bufferedReader !=
	 * null) { int cp; while ((cp = bufferedReader.read()) != -1) { sb.append((char)
	 * cp); } bufferedReader.close(); } } in.close(); } catch (Exception e) { throw
	 * new RuntimeException("Exception while calling URL:"+ myURL, e); } return
	 * sb.toString(); }
	 */

	public String callWebsite(String stringURL) {
		// logger.info("Calling Website : " + stringURL);
		Document doc = null;
		String content = null;
		try {
			doc = Jsoup.connect(stringURL).get();
			content = doc.html();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/*
		 * try ( WebClient webClient = new
		 * WebClient(BrowserVersion.INTERNET_EXPLORER_11)) {
		 * webClient.getOptions().setThrowExceptionOnScriptError(false); HtmlPage page =
		 * null; try { logger.info("URL = " + stringURL); page =
		 * webClient.getPage(stringURL);
		 *
		 * } catch (MalformedURLException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); } catch (Exception e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); } if (page != null) { content =
		 * page.asText(); } }
		 */
		return content;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		WikiScrapeAndInsert instance = new WikiScrapeAndInsert(args[0]);
		// instance.processLevel1();
		// instance.testLevel2();
		instance.processLevel2();
		// logger.info("ORI ->" + WikiScrapeAndInsert.MW);
		// Document doc = Jsoup.parse(WikiScrapeAndInsert.MW);
		// instance.removeComments(doc);
		// logger.info("NEW ->" + doc);

	}
}
