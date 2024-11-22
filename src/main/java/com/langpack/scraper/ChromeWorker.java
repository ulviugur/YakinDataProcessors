package com.langpack.scraper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.langpack.common.BasicClass;
import com.langpack.common.GlobalUtils;
import com.langpack.integration.TDKChapterItem;
import com.langpack.integration.TDKMeaningItem;
import com.langpack.integration.TDKWordWrapper;
import com.langpack.structure.WordGenerator;
import com.mongodb.client.MongoCollection;

public class ChromeWorker implements Runnable {

	public static final Logger log4j = LogManager.getLogger("ChromeWorker");

	ChromeHeadless myChrome = null;
	WordGenerator generator = null;
	String ChromeHeadlessConfigFile = null;
	String baseURL = null;
	MongoCollection<Document> rawBenzerColl = null;
	private BlockingQueue<String> taskQueue = new LinkedBlockingQueue<>();

	Integer workerIndex = null;

	public ChromeWorker(String chromeConfigFile, String tmpURL, MongoCollection<Document> tmpColl, Integer index) {
		myChrome = new ChromeHeadless(chromeConfigFile);
		baseURL = tmpURL;
		rawBenzerColl = tmpColl;
		workerIndex = index;

	}

	public boolean addTask(String word) throws InterruptedException {

		// Add the task to the queue
		taskQueue.add(word);
		log4j.info("Queue [{}] : {}", workerIndex, GlobalUtils.convertArrayToString(taskQueue));
		return true;
	}

	public int getTaskSize() {
		return taskQueue.size();
	}

	@Override
	public void run() {
		String word = null;
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (!taskQueue.isEmpty()) {
			// Retrieve the task from the queue
			word = taskQueue.poll();
			scrapeThesWords(word);
		}
	}

	public Document scrapeThesWords(String word) {
		Document retval = new Document();
		retval.put("word", word);

		Boolean pageFound = false;

		String scrapeURL = String.format("%s%s", baseURL, word);
		log4j.info("Scraping with URL :" + scrapeURL);

		String page = null;
		boolean capthca = false;

		myChrome.gotoURL(scrapeURL);

		while (!pageFound) {
			// page = GlobalUtils.callWebsite(scrapeURL);

			page = myChrome.getPageSource();

			if (page.contains("Doğrulama")) {
				capthca = true;
				log4j.info("Güvenlik formu");
				WebElement result = myChrome.waitForCaptcha(10, "recaptcha-anchor-label");
				log4j.info("Returned : " + result);

			} else {
				capthca = false;
				org.jsoup.nodes.Document doc = Jsoup.parse(page);
				Elements mainElements = doc.select("div.entry-content-main li a");
				if (mainElements != null || page.contains("TDK Güncel Türkçe Sözlük")) {
					pageFound = true;
					break;
				} else {
					log4j.info("Word block could not be found, check puppet browser");
				}
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

		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public static void main(String[] args) {
		int Q_SIZE = 10;

		ArrayList<ChromeWorker> workerArray = new ArrayList<>();
		for (Integer i = 0; i < Q_SIZE; i++) {
			ChromeWorker queWorker = new ChromeWorker(args[0], "", null, 0);
			workerArray.add(queWorker);
			Thread t1 = new Thread(queWorker);
			t1.start();
		}
		System.out.println("classpath=" + System.getProperty("java.class.path"));

	}
}
