package com.langpack.scraper;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.integration.TDKLevel2Parser;
import com.langpack.integration.TDKWordWrapper;

public class ChromeHeadless {
	public static final Logger log4j = LogManager.getLogger("ChromeHeadless");
	WebDriver driver = null;
	boolean openBrowser = false;
	TDKLevel2Parser parser = null;
	String baseURL = null;
	String chromeDriverLocation = null;
	String chromeDriverArguments = null;
	ConfigReader cfg = null;

	public ChromeHeadless(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		baseURL = cfg.getValue("Download.Site");
		chromeDriverLocation = cfg.getValue("chromeDriverLocation");
		chromeDriverArguments = cfg.getValue("chromeDriverArguments");

		// Setting system properties of ChromeDriver
		System.setProperty("webdriver.chrome.driver", chromeDriverLocation);

		ChromeOptions options = new ChromeOptions();
		// options.addArguments("--headless", "--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");
		String[] tmpArguments = chromeDriverArguments.split(";");
		options.addArguments(tmpArguments);
		// options.addArguments("--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");

		driver = new ChromeDriver(options);

		// Creating an object of ChromeDriver driver = new ChromeDriver();
		driver.manage().window().maximize();

		// Deleting all the cookies driver.manage().deleteAllCookies();

		// Specifiying pageLoadTimeout and Implicit wait
		// driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
		// driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

		parser = new TDKLevel2Parser();

	}

	public void closeChromeHeadless() {
		driver.close();
	}

	public void initializeForTDKHTMLScrape() {
		log4j.warn("Initializing ChromeHeadless ...");
		while (true) {
			try {
				// launching the specified URL
				driver.get(baseURL);
				break;
			} catch (org.openqa.selenium.TimeoutException ex) {
				log4j.warn("Timeout is hit, retrying again ..");
				ex.printStackTrace();
			}
		}
	}

	// use a word to read the content
	public TDKWordWrapper scrapeTDKContentForWord(String word) {
		TDKWordWrapper retval = null;

		log4j.info(String.format("Looking up word : %s", word));

		WebElement editSearch = driver.findElement(By.className("tdk-search-input"));
		editSearch.clear();
		editSearch.sendKeys(word);

		WebElement btnSearch = driver.findElement(By.id("tdk-search-btn"));
		btnSearch.click();

		try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// retval = parser.parseHTMLContent(word, driver);
		// retval = parser.parseHTMLContent(source);

		return retval;
	}

	// scrape all the words shown when the key is put in. A key is not necessarily a
	// word, it is a test keyword for drilling
	public ArrayList<String> scrapeTDKWordsWithKey(String key) {
		log4j.info(String.format("Looking for key : %s", key));

		ArrayList<String> retval = new ArrayList<>();
		// if (!driver.getCurrentUrl().equals(ORI_URL)) {
		while (true) {
			try {
				// launching the specified URL
				driver.get(baseURL);
				break;
			} catch (org.openqa.selenium.TimeoutException ex) {
				log4j.warn("Timeout is hit, retrying again ..");
				ex.printStackTrace();
			}
		}
		// }

		// Locating the elements using name locator for the text box
		WebElement editSearch = driver.findElement(By.className("tdk-search-input"));
		editSearch.clear();
		editSearch.sendKeys(key);
		editSearch.clear();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		editSearch.sendKeys(key);

		int count = 1;
		boolean moveon = true;

		do {
			String elementPath = String.format("//*[@id=\"autocmp\"]/div[%s]", count);
			WebElement line = null;

			try {
				line = driver.findElement(By.xpath(elementPath));
				retval.add(line.getText());
				count++;
			} catch (NoSuchElementException ex) {
				moveon = false;
			}

		} while (moveon);
		log4j.info(String.format("Found entries : %s", GlobalUtils.convertArraytoString(retval)));
		return retval;
	}

	public void downloadBook() {
		// launching the specified URL
		driver.get("https://b-ok.cc/book/1193456/2de679");

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Locating the elements using name locator for the text box
		WebElement btnDownload = driver
				.findElement(By.xpath("/html/body/table/tbody/tr[2]/td/div/div/div/div[2]/div[2]/div[1]/div[1]/div/a"));
		btnDownload.click();

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		log4j.info("testing.........");

		ChromeHeadless instance = new ChromeHeadless(args[0]);
		instance.initializeForTDKHTMLScrape();

		instance.scrapeTDKContentForWord("avcı");
		/*
		 * instance.scrapeTDKContentForWord("aydın");
		 * instance.scrapeTDKContentForWord("ilahi");
		 * instance.scrapeTDKContentForWord("atak");
		 * instance.scrapeTDKContentForWord("bar");
		 */

		// instance.scrapeTDKContentForWord("bar");

	}
}