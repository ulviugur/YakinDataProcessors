package com.langpack.scraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

	public static final Logger log4j = LogManager.getLogger("DNRBooksLookup");

	Boolean pageFound = false;
	PageParser parser = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String baseURL = "https://www.benzerkelimeler.com/kelime/";

	String mongoURL = "mongodb://localhost:27017";

	String ledgerFolder = null;

	MongoCollection<Document> rawBenzerColl = null;
	MongoCollection<Document> wordsColl = null;
	
	ChromeHeadless myChrome = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLFileIterator xlIter = null;
	File xlFile = null;
	XSSFWorkbook wb;
	XSSFSheet ledgerSheet = null;
	int currRowNum;

	TreeSet<String> ledger = new TreeSet<String>();

	public BenzerScrapeIntoMongo(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		ledgerFolder  = cfg.getValue("LedgerFolder");
		File xlFolder = new File(ledgerFolder);
		xlIter = new XLFileIterator(xlFolder);
		xlFile = xlIter.getNextImportFile();
		wb = xlIter.openWorkbook();
		ledgerSheet = xlIter.getSheet("BenzerLedger");

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		rawBenzerColl = database.getCollection("benzer_raw");
		wordsColl = database.getCollection("words");
		

		
		String chromeConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		myChrome = new ChromeHeadless(chromeConfigFile);

		int loadedWords = loadLedger();
		log4j.info("Loaded {} words from ledger file ..", loadedWords);

	}

	public int loadLedger() {
		int skipLabelRow = 1;
		currRowNum = skipLabelRow;
		while (true) {
			XSSFRow rowData = ledgerSheet.getRow(currRowNum);

			if (rowData == null) {
				break;
			} else {
				XSSFCell cellWord = rowData.getCell(0);
				String word = cellWord.getStringCellValue();
				ledger.add(word);
			}

			currRowNum++;
		}
		return currRowNum - skipLabelRow;
	}

	public void runWords() {

		int runonly = -1;
		int runcount = 0;
		FindIterable<Document> select = wordsColl.find();
		MongoCursor<Document> iter = select.iterator();

		while (iter.hasNext()) {
			Document tmp = iter.next();
			String word = (String) tmp.get("word");
			if (ledger.contains(word)) {
				log4j.info("[{}] Already processed word {}, skipping ! ..", word);
				continue;
			} else {
				log4j.info("[{}] Processing word {} ..",runcount, word);
			}

			scrapeThesWords(word);

			runcount++;
			
			saveSheet();
			
			runonly--;
			if (runonly == 0) {
				break;
			}
		}

		saveSheet();

		// Document tmp = scrapeThesWords("adım");

		log4j.info("Process completed ..");
	}
	private void addWordtoLedger(String _word) {
		XSSFRow rowData = ledgerSheet.createRow(currRowNum);

		XSSFCell cellWord = rowData.createCell(0);
		cellWord.setCellValue(_word);
		currRowNum++;
		saveSheet();
	}

	public boolean saveSheet() {
		FileOutputStream fileOut;
		try {
			log4j.info("Saving file {}", xlFile.getAbsolutePath());
			fileOut = new FileOutputStream(xlFile);
			wb.write(fileOut);
			fileOut.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public Document scrapeThesWords(String word) {
		Document retval = new Document();
		retval.put("word", word);
		
		if (word != null) {
			addWordtoLedger(word);
			
			return null;
		}
		String scrapeURL = String.format("%s%s", baseURL, word);
		log4j.info(scrapeURL);

		String page = null;

		while (true) {
			//page = GlobalUtils.callWebsite(scrapeURL);
			page = myChrome.getURLContent(scrapeURL);

			org.jsoup.nodes.Document doc = Jsoup.parse(page);
			Elements mainElements = doc.select("div.entry-content-main li a");
			if (mainElements != null || page.contains("TDK Güncel Türkçe Sözlük")) {
				addWordtoLedger(word);
				break;
			}
		}

		ArrayList<String> mainList = new ArrayList<String>();
		ArrayList<Document> docList = new ArrayList<Document>();

		if (pageFound) {

			// System.out.print(page);
			org.jsoup.nodes.Document doc = Jsoup.parse(page);
			Elements mainElements = doc.select("div.entry-content-main li a");

			for (Iterator<Element> iterator = mainElements.iterator(); iterator.hasNext();) {
				Element item = (Element) iterator.next();
				String wordTmp = item.text();
				mainList.add(wordTmp);
			}

			Elements subElements = doc.select("div.entry-content-sub-content");

			int subCount = 0;
			for (Iterator<Element> iterator = subElements.iterator(); iterator.hasNext();) {

				Element subElement = (Element) iterator.next();

				Elements endElements = subElement.select("li a");
				Document subWordContent = new Document();
				subWordContent.put("subword", mainList.get(subCount));
				ArrayList<String> subItems = new ArrayList<String>();
				for (Iterator<Element> endIterator = endElements.iterator(); endIterator.hasNext();) {
					Element endItem = (Element) endIterator.next();
					String item = endItem.text();
					// log4j.info("{} -> {} ", mainList.get(subCount), item);
					subItems.add(item);
				}
				subWordContent.put("subitems", subItems);
				docList.add(subWordContent);
				// log4j.info(subWordContent.toJson());
				subCount++;
			}
			retval.put("sublist", docList);
			log4j.info(retval.toJson());

			rawBenzerColl.insertOne(retval);

			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public static void main(String[] args) {
		BenzerScrapeIntoMongo instance;
		instance = new BenzerScrapeIntoMongo(args[0]);
		instance.runWords();
		// instance.scrapeThesWords("Abazaca");
	}

}
