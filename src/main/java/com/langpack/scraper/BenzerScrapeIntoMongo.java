package com.langpack.scraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.XLFileIterator;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class BenzerScrapeIntoMongo {

	public static final Logger log4j = LogManager.getLogger("BenzerScrapeIntoMongo");
	PageParser parser = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String baseURL = "https://www.benzerkelimeler.com/kelime/";

	String mongoURL = "mongodb://localhost:27017";

	String ledgerFolder = null;

	MongoCollection<Document> rawBenzerColl = null;
	MongoCollection<Document> wordsColl = null;

	// ChromeHeadless myChrome = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLFileIterator xlIter = null;
	File xlFile = null;
	XSSFWorkbook wb;
	XSSFSheet ledgerSheet = null;

	Integer workerSize = null;

	int currRowNum;

	TreeSet<String> ledger = new TreeSet<String>();

	ArrayList<ChromeWorker> workers = new ArrayList<ChromeWorker>();

	public BenzerScrapeIntoMongo(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		ledgerFolder = cfg.getValue("LedgerFolder");
		File xlFolder = new File(ledgerFolder);
		xlIter = new XLFileIterator(xlFolder);
		xlFile = xlIter.getNextImportFile();
		wb = xlIter.openWorkbook();
		ledgerSheet = xlIter.getSheet("BenzerLedger");

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		rawBenzerColl = database.getCollection("benzer_raw");
		wordsColl = database.getCollection("words");

		workerSize = Integer.parseInt(cfg.getValue("WorkerSize", "1"));

		String chromeConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		// myChrome = new ChromeHeadless(chromeConfigFile);

		int loadedWords = loadLedger();
		log4j.info("Loaded {} words from ledger file ..", loadedWords);

		for (int i = 0; i < workerSize; i++) {
			ChromeWorker worker = new ChromeWorker(chromeConfigFile, baseURL, rawBenzerColl, i);
			workers.add(worker);
			Thread thread = new Thread(worker); // Wrap ChromeWorker in a Thread
			thread.start(); // Start the Thread
		}

	}

	public int loadLedger() {
		FindIterable<Document> collItems = rawBenzerColl.find();
		MongoCursor<Document> iter = collItems.cursor();
		int count = 0;
		while (iter.hasNext()) {
			Document item = iter.next();
			String word = (String) item.get("word");
			ledger.add(word);
			count++;
		}
		return count;
	}

	/*
	 * Using an XL Sheet in a mult-worker enviroment is not possible, changed it to
	 * a mongo collection lookup public int loadLedger() { int skipLabelRow = 1;
	 * currRowNum = skipLabelRow; while (true) { XSSFRow rowData =
	 * ledgerSheet.getRow(currRowNum);
	 * 
	 * if (rowData == null) { break; } else { XSSFCell cellWord =
	 * rowData.getCell(0); String word = cellWord.getStringCellValue();
	 * ledger.add(word); }
	 * 
	 * currRowNum++; } return currRowNum - skipLabelRow; }
	 */

	public void runWords() {

		int runonly = 1000;
		int runcount = 0;
		FindIterable<Document> select = wordsColl.find();
		MongoCursor<Document> iter = select.iterator();

		while (iter.hasNext()) {
			Document tmp = iter.next();
			String word = (String) tmp.get("word");

			if (word.contains("(")) {
				// dont even try the words with brackets in it
				continue;
			}

			if (ledger.contains(word)) {
				log4j.info("[{}] Already processed word {}, skipping ! ..", word);
				continue;
			} else {
				log4j.info("[{}] Adding word to queue : {} ..", runcount, word);
			}

			int workerSelector = runcount % workerSize;

			// try adding to the tasklist of the designated worker; if not just wait a bit
			while(true) {
				ChromeWorker worker = workers.get(workerSelector);
				try {
					boolean accepted = worker.addTask(word);
					if (accepted) {
						break;
					} else {
						Thread.sleep(5000);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			runcount++;

			runonly--;
			if (runonly == 0) {
				break;
			}

		}

		log4j.info("Process completed ..");
	}
//
//	private void addWordtoLedger(String _word) {
//		XSSFRow rowData = ledgerSheet.createRow(currRowNum);
//
//		XSSFCell cellWord = rowData.createCell(0);
//		cellWord.setCellValue(_word);
//		currRowNum++;
//		saveSheet();
//	}

//	public boolean saveSheet() {
//		FileOutputStream fileOut;
//		try {
//			log4j.info("Saving file {}", xlFile.getAbsolutePath());
//			fileOut = new FileOutputStream(xlFile);
//			wb.write(fileOut);
//			fileOut.close();
//			return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
//	}

//	public Document scrapeThesWordsOld(String word) {
//		Document retval = new Document();
//		retval.put("word", word);
//
//		Boolean pageFound = false;
//
//		String scrapeURL = String.format("%s%s", baseURL, word);
//		log4j.info("Scraping with URL :" + scrapeURL);
//
//		String page = null;
//		boolean capthca = false;
//		
//		myChrome.gotoURL(scrapeURL);
//		
//		while (!pageFound) {
//			// page = GlobalUtils.callWebsite(scrapeURL);
//
//			page = myChrome.getPageSource();
//
//			if (page.contains("Doğrulama")) {
//				capthca = true;
//				log4j.info("Güvenlik formu");
//				WebElement result = myChrome.waitForCaptcha(10, "recaptcha-anchor-label");
//				log4j.info("Returned : " + result);
//
//			} else {
//				capthca = false;
//				org.jsoup.nodes.Document doc = Jsoup.parse(page);
//				Elements mainElements = doc.select("div.entry-content-main li a");
//				if (mainElements != null || page.contains("TDK Güncel Türkçe Sözlük")) {
//					pageFound = true;
//					break;
//				} else {
//					log4j.info("Word block could not be found, check puppet browser");
//				}
//			}
//		}
//
//		ArrayList<String> mainList = new ArrayList<String>();
//		ArrayList<Document> docList = new ArrayList<Document>();
//
//		if (pageFound) {
//
//			// System.out.print(page);
//			org.jsoup.nodes.Document doc = Jsoup.parse(page);
//			Elements mainElements = doc.select("div.entry-content-main li a");
//
//			for (Iterator<Element> iterator = mainElements.iterator(); iterator.hasNext();) {
//				Element item = (Element) iterator.next();
//				String wordTmp = item.text();
//				mainList.add(wordTmp);
//			}
//
//			Elements subElements = doc.select("div.entry-content-sub-content");
//
//			int subCount = 0;
//			for (Iterator<Element> iterator = subElements.iterator(); iterator.hasNext();) {
//
//				Element subElement = (Element) iterator.next();
//
//				Elements endElements = subElement.select("li a");
//				Document subWordContent = new Document();
//				subWordContent.put("subword", mainList.get(subCount));
//				ArrayList<String> subItems = new ArrayList<String>();
//				for (Iterator<Element> endIterator = endElements.iterator(); endIterator.hasNext();) {
//					Element endItem = (Element) endIterator.next();
//					String item = endItem.text();
//					// log4j.info("{} -> {} ", mainList.get(subCount), item);
//					subItems.add(item);
//				}
//				subWordContent.put("subitems", subItems);
//				docList.add(subWordContent);
//				// log4j.info(subWordContent.toJson());
//				subCount++;
//			}
//			retval.put("sublist", docList);
//			log4j.info(retval.toJson());
//
//			rawBenzerColl.insertOne(retval);
//
//		}
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return retval;
//	}

	public static void main(String[] args) {
		BenzerScrapeIntoMongo instance;
		instance = new BenzerScrapeIntoMongo(args[0]);
		instance.runWords();
		// instance.scrapeThesWords("Abazaca");
	}

}
