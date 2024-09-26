package com.langpack.integration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import com.langpack.common.BasicClass;
import com.langpack.scraper.DictWord;
import com.langpack.structure.WordGenerator;

// Read diff records from the database, scrape them and insert into another table
public class TDKScraper extends BasicClass {
	public static final Logger log4j = LogManager.getLogger("TDKScraper");
	private String sourceFolder = null;
	private TDKLevel2Parser level2Parser = null;

	// String[] WIKI_KEYWORDS = new String[] {"^&"};
	String[] WIKI_KEYWORDS = new String[] { "\\.", "\\d+", ">", "\\[", "\\]", ":", "\"", "\\(", "\\)", "''", "=", ";",
			",", "^\\'", "\\'$", "^\\*", "^\\\\/*", "^&", "^%", "^!", "^ˈ", "^\\?", "\\?$", "^-", "^<", "^_", "_$", "`",
			"^\\/", "^\\^", "#", "\\s+", };
	TreeSet<String> LOOKUP_LIST = new TreeSet<>();

	TreeMap<String, DictWord> TDKWordDictionary = new TreeMap<>();
	TreeMap<String, DictWord> WordLedger = new TreeMap<>();
	boolean dictionaryLoaded = false;
	Boolean loadLedger = false;

	String errorPageIndicator = null;
	String exportFolder = null;
	String importFileName = null;

	public TDKScraper(String cfgFileName) {
		super(cfgFileName);

		errorPageIndicator = cfg.getValue("Download.ErrorPageIndicator");
		sourceFolder = cfg.getValue("Download.Site");
		exportFolder = cfg.getValue("Export.Folder");
		importFileName = cfg.getValue("Import.File");

		loadLedger = Boolean.parseBoolean(cfg.getValue("LoadLedger"));

		level2Parser = new TDKLevel2Parser();

		log4j.info("Loading dictionary tables ..");

		if (loadLedger) {
			loadLedger();
		}

	}

	public Connection getDBconn() {
		return DBconn;
	}

	public void setDBconn(Connection dBconn) {
		DBconn = dBconn;
	}

	private String stripKeywords(String ori) {
		String retval = ori;

		// run it for 3 times to clean multiple problems
		while (true) {
			String str1 = retval;
			for (String key : WIKI_KEYWORDS) {
				retval = retval.replaceAll(key, " ");
				// logger.info("key:<" + key + ">STAT:<" + retval + ">");
			}
			retval = retval.trim();
			if (str1.equals(retval)) {
				break;
			}
		}

		return retval;
	}

	public DictWord lookupWordinTDKAndInsert(String word, String source, boolean forceLookup) {
		DictWord retval = null;
		DictWord objTDK = TDKWordDictionary.get(word);
		DictWord objLedger = WordLedger.get(word);

		boolean lookupNeeded = false;
		if (forceLookup) {
			lookupNeeded = true;
		} else {
			if (objTDK != null) {
				retval = objTDK;
			} else {
				if (objLedger != null && "Y".equals(objLedger.getProcessed())) {
					lookupNeeded = false;
				} else {
					lookupNeeded = true;
				}

			}
		}

		if (lookupNeeded) {

			try {
				psDelete1.setString(1, word);
				int deleted = psDelete1.executeUpdate();
				log4j.debug(deleted + " record deleted ..");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				psDelete2.setString(1, word);
				int deleted = psDelete2.executeUpdate();
				log4j.debug(deleted + " record deleted ..");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Response res = null;
			Document doc = null;

			String fullURL = String.format("%s/gts?ara=%s", sourceFolder, word);

			try {
				res = Jsoup.connect(fullURL).header("Accept-Encoding", "gzip, deflate, br")
						.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
						.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
						.userAgent(
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
						.execute();

				doc = res.parse();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Element content = doc.body();

			if (content != null) {
				// log4j.info("+++++ Header found in " + fullURL);
				java.util.Date time = new java.util.Date();
				String found = "Y";
				if (content.toString().contains("\"error\":\"Sonuç bulunamadı")) {
					found = "N";
				}

				// Insert into WORDS_LOOKUPLEDGER
				try {
					psInsert2.setString(1, word);
					psInsert2.setString(2, "Y");
					psInsert2.setString(3, found);
					psInsert2.setString(4, source);
					psInsert2.executeUpdate();

					log4j.debug(String.format("Inserted word '%s' to the ledger table", word));
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}

				// lookup word can be different than the original word
				String tdk_word = level2Parser.getLevel2Word();
				if (tdk_word != null) {
					log4j.info(String.format("Found content : %s", tdk_word));
					word = tdk_word;
				}
				String timeStr = format.format(time);

				// Insert into TDK_WORDS
				try {
					retval = new DictWord(word);
					retval.setSource(sourceFolder);
					retval.setURL(fullURL);
					retval.setFound(found);
					retval.setProcessed("Y");
					retval.setLevel2(content.toString());
					retval.setImportTime(timeStr);

					psInsert1.setString(1, word);
					psInsert1.setString(2, sourceFolder);
					psInsert1.setString(3, fullURL);
					psInsert1.setString(4, found);
					psInsert1.setString(5, content.toString());
					psInsert1.setString(6, timeStr);
					psInsert1.executeUpdate();

					log4j.debug(String.format("Inserted word '%s' to the TDK table", word));
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log4j.error(String.format("word[%s], sourceFolder[%s], fullURL[%s], content[%s], time[%s]",
							word.length(), sourceFolder.length(), fullURL.length(), content.toString().length(),
							timeStr.length()));

				}

				if ("Y".equals(found)) {
					objTDK = new DictWord(word);
					objTDK.setFound("Y");
					objTDK.setImportTime(format.format(time));
					objTDK.setLevel2(content.toString());
					objTDK.setProcessed("Y");
					objTDK.setSource(source);
					objTDK.setUpdateTime(null);
					objTDK.setWord(word);

					TDKWordDictionary.put(word, objTDK);
					WordLedger.put(word, objTDK);
				} else {
					log4j.debug(String.format("%s could not be found ..", word));
				}

			}
		}
		return retval;
	}

	// Running a TDK lookup based on WordGenerator words
	public void lookupGenereatedWords(int wordLength, String suffix) {

		int totalFound = 0;
		int roundFound = 0;
		if (suffix == null) {
			suffix = "";
		}

		log4j.info(String.format("Running generatedWords %s", wordLength));
		WordGenerator wg = new WordGenerator(wordLength);

		String tmpWord = null;
		int count = 0;
		while ((tmpWord = wg.nextWord()) != null) {
			tmpWord = tmpWord + suffix;
			log4j.info(String.format("Looking for %s", tmpWord));
			DictWord retval = lookupWordinTDKAndInsert(tmpWord, "WordGenerator", false);
			// DictWord retval = null;
			if (retval.isFound()) {
				roundFound++;
				totalFound++;
				log4j.info(
						String.format("Found word : %s [Found/Tried >> %s / %s]", retval.getWord(), roundFound, count));
				log4j.info(String.format("Total found : %s", totalFound));
			}
			count++;
			if ((count % 1000) == 0) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void loadLedger() {
		int count = 0;
		ResultSet records = null;

		try {
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			log4j.info(String.format("Loading records from TDK reference table"));
			while (records.next()) {
				String id = records.getString("id");
				String word = records.getString("word");
				String level2 = records.getString("level2");
				String found = records.getString("found");

				DictWord rec = new DictWord(word);
				rec.setId(id);
				rec.setWord(word);
				rec.setLevel2(level2);
				rec.setFound(found);

				TDKWordDictionary.put(word, rec);
				count++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info(String.format("Loaded [%s] records from TDK reference table", count));

		count = 0;
		try {
			records = psSelect2.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			log4j.info(String.format("Loading records from LEDGER reference table"));
			while (records.next()) {
				String id = records.getString("id");
				String word = records.getString("word");
				String processed = records.getString("processed");
				String found = records.getString("found");

				DictWord rec = new DictWord(word);
				rec.setId(id);
				rec.setWord(word);
				rec.setProcessed(processed);
				rec.setFound(found);

				WordLedger.put(word, rec);
				count++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info(String.format("Loaded [%s] records from TDK reference table", count));
		dictionaryLoaded = true;
	}

	public void test() {
		String url = "https://sozluk.gov.tr/";
		Response resp = null;
		try {
			resp = Jsoup.connect(url) //
					.timeout(30000) //
					.method(Method.GET) //
					.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// * Find the form
		Document responseDocument = null;
		try {
			responseDocument = resp.parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Element potentialForm = responseDocument.select("form[id$=tdk-srch-form]").first();
		FormElement form = (FormElement) potentialForm;

		// * Fill in the form and submit it
		// ** Search Type
		Element radioButtonListSearchType = form.select("[name$=RadioButtonList_SearchType]").first();
		radioButtonListSearchType.attr("checked", "checked");

		// ** Name search
		Element textBoxNameSearch = form.select("[name$=TextBox_NameSearch]").first();
		textBoxNameSearch.val("cali");

		// ** Submit the form
		Document searchResults = null;
		try {
			searchResults = form.submit().cookies(resp.cookies()).post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// * Extract results (entity numbers in this sample code)
		for (Element entityNumber : searchResults
				.select("table[id$=SearchResults_Corp] > tbody > tr > td:first-of-type:not(td[colspan=5])")) {
			System.out.println(entityNumber.text());
		}
	}

	// scrape all words loaded from a database table and scrape using ChromeHeadless
//	public void scrapeWordPages() {
//		ChromeHeadless chrome = new ChromeHeadless(true);
//		chrome.initializeForTDKHTMLScrape();
//
//		int count = 0; int tmp1 = -1; int tmp2 = -1;
//		ResultSet records = null;
//
//		try {
//			records = psSelect1.executeQuery();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//
//			while (records.next()) {
//				String id = records.getString("id");
//				String word = records.getString("word");
//				log4j.info(String.format("Processing records from TDK_SCRAPE table => %s, %s", id, word));
//
//				ArrayList<TDKMeaningItem> items = chrome.scrapeTDKContentForWord(word);
//				for (int i = 0; i < items.size(); i++) {
//					TDKMeaningItem item = items.get(i);
//					psInsert1.setString(1, item.getWord());
//					psInsert1.setString(2, item.getChapterId());
//					psInsert1.setString(3, item.getWordType());
//					psInsert1.setString(4, item.getMeaningId());
//					psInsert1.setString(5, item.getMeaning());
//					psInsert1.setString(6, GlobalUtils.getCurrentDateString());
//					tmp1 = psInsert1.executeUpdate();
//
//					if (item.getSample() != null) {
//						psInsert2.setString(1, item.getWord());
//						psInsert2.setString(2, item.getChapterId());
//						psInsert2.setString(3, item.getWordType());
//						psInsert2.setString(4, item.getMeaningId());
//
//						psInsert2.setString(5, item.getSample());
//						psInsert2.setString(6, item.getSampleAuthor());
//						psInsert2.setString(7, GlobalUtils.getCurrentDateString());
//						tmp2 = psInsert2.executeUpdate();
//					}
//				}
//
//				count++;
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.exit(-1);
//		}
//
//	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		TDKScraper instance = new TDKScraper(args[0]);
		// instance.scrapeWordPages();
		instance.lookupGenereatedWords(2, "");
	}

}
