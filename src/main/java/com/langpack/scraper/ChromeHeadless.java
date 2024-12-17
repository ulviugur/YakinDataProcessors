package com.langpack.scraper;

import java.time.Duration;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;

public class ChromeHeadless {
	public static final Logger log4j = LogManager.getLogger("ChromeHeadless");
	WebDriver driver = null;
	boolean openBrowser = false;

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

		// driver = new ChromeDriver();
		driver = new ChromeDriver(options);

		// Creating an object of ChromeDriver driver = new ChromeDriver();
		driver.manage().window().maximize();

		// Deleting all the cookies driver.manage().deleteAllCookies();

		// Specifiying pageLoadTimeout and Implicit wait
		// driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
		// driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
	}

	public String getURLContent(String url) {

		// driver.get("https://1000kitap.com/ara?q=Harnilton+Edmond&bolum=yazarlar&hl=tr");
		driver.get(url);

		String pageSource = driver.getPageSource();

		return pageSource;
	}

	public void gotoURL(String url) {

		// driver.get("https://1000kitap.com/ara?q=Harnilton+Edmond&bolum=yazarlar&hl=tr");
		driver.get(url);

	}
	
	public String getPageSource() {
		String pageSource = driver.getPageSource();
		return pageSource;
	}
	
	public WebElement waitForCaptcha(Integer waitSeconds, String capthaElementIdentifier) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		WebElement element = null;
		try {
			// Replace with a unique element that appears only after CAPTCHA is completed
			element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(capthaElementIdentifier)));
			System.out.println("CAPTCHA completed, proceeding with the rest of the script.");

		} catch (org.openqa.selenium.TimeoutException e) {
			// e.printStackTrace();
			System.out.println("Timed out waiting for CAPTCHA to be completed.");
		}
		return element;
	}

	public void closeChromeHeadless() {
		driver.close();
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
		log4j.info(String.format("Found entries : %s", GlobalUtils.convertArrayToString(retval)));
		return retval;
	}

	public static void main(String[] args) {
		log4j.info("Testing Headless Chrome ..");
		ChromeHeadless instance = new ChromeHeadless(args[0]);
		instance.getURLContent("example.com");
	}
}