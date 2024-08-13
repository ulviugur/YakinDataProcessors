package com.langpack.integration;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.google.common.io.Files;
import com.langpack.common.BasicClass;
import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;

public class ZLibraryChromeFileDownloader extends BasicClass {

	String chromeDriverLocation = null;
	String chromeDriverArguments = null;
	String loginURL = null;
	String chromeHeadlessConfigFile = null;

	String downloadDirName = null;
	String destDirName = null;

	static Integer POOL_SIZE = 0;
	ArrayList<WebDriver> driverList = new ArrayList<>();

	public static ArrayList<String> EXTENSION_LIST = GlobalUtils.toStringArrayList(new String[] { "pdf", "epub" }); // lowercase
																													// extensions

	public ZLibraryChromeFileDownloader(String cfgFileName) {
		super(cfgFileName);

		loginURL = cfg.getValue("Login.Site");

		POOL_SIZE = Integer.parseInt(cfg.getValue("pool.Size"));

		chromeHeadlessConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		ConfigReader chromeCfg = new ConfigReader(chromeHeadlessConfigFile);

		chromeDriverLocation = chromeCfg.getValue("chromeDriverLocation");
		chromeDriverArguments = chromeCfg.getValue("chromeDriverArguments");

		// Setting system properties of ChromeDriver
		System.setProperty("webdriver.chrome.driver", chromeDriverLocation);

		downloadDirName = cfg.getValue("Download.Dir", "C:\\Users\\ulvi\\Downloads");
		destDirName = cfg.getValue("Dest.Dir", "S:\\Ulvi\\wordspace\\Wordlet\\_BOOKS\\");

		// ChromeOptions options = new ChromeOptions();
		// options.addArguments("--headless", "--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");
		// String[] tmpArguments = chromeDriverArguments.split(";");
		// options.addArguments(tmpArguments);
		// options.addArguments("--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");

		// driver = new ChromeDriver(options);
		// driver = new ChromeDriver();

		// Creating an object of ChromeDriver driver = new ChromeDriver();
		// driver.manage().window().maximize();
		// downloader = new
		// ZLibraryDownloader("S:\\Ulvi\\wordspace\\Wordlet\\config\\ZLibrary_Downloader_1.cfg");

		for (int i = 0; i < POOL_SIZE; i++) {
			WebDriver driver = new ChromeDriver();
			login(driver);
			driverList.add(driver);
		}

	}

	public void login(WebDriver _driver) {

		ConfigReader chromeCfg = new ConfigReader(chromeHeadlessConfigFile);

		chromeDriverLocation = chromeCfg.getValue("chromeDriverLocation");
		chromeDriverArguments = chromeCfg.getValue("chromeDriverArguments");

		_driver.get(loginURL);

		WebElement elementName = _driver.findElement(By.name("email"));
		elementName.sendKeys("ulvi.ugur@gmail.com");

		WebElement elementPassword = _driver.findElement(By.name("password"));
		elementPassword.sendKeys("Reuters50$");

		WebElement elementSubmit = _driver.findElement(By.name("submit"));
		elementSubmit.click();

		log4j.info("LoggedIn");

	}

	public boolean moveFilesToDest(ArrayList<File> sourceFiles, File destDir) {
		log4j.info("Moving " + sourceFiles.size() + " files to " + destDir.getAbsolutePath());
		boolean retval = false;

		for (File sourceFile : sourceFiles) {
			File destFile = new File(destDir, sourceFile.getName());

			log4j.info("Moving " + sourceFile + " to " + destFile);

			try {
				Files.move(sourceFile, destFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return retval;
	}

	private void gotoURLSearchPage(WebDriver driver, String destURL, String markerString) { // markerString is an
																							// indicative phrase which
		// confirms that the page is loaded
		try {

			Thread.sleep(500);
			driver.get(destURL);
			boolean displayed = false;

			int tried = 0;
			while (!displayed) {
				displayed = driver.findElement(By.xpath(markerString)).isDisplayed();

				Thread.sleep(500);
				if (tried > 20) {
					log4j.error("Could not load the page in " + tried + " tries, exitting !!");
					System.exit(-1);
				}
				tried++;
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void runOneCycle() {

		ResultSet records = null;
		try {
			psSelect1.setString(1, Integer.toString(POOL_SIZE + 1));
			log4j.info("Running : " + selectString1);
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int count = 0;
		ArrayList<String> idList = new ArrayList<>();

		try {
			WebDriver driver = new ChromeDriver();
			login(driver);
			while (records.next()) {
				String id = records.getString("ID");
				String fullURL = records.getString("FULLURL");
				String baseURL = records.getString("BASEURL");
				String url = records.getString("DOWNLOAD_URL");

				idList.add(id);

				// WebDriver driver = driverList.get(count);

				String downloadURL = baseURL + url;

				log4j.info("Processing [" + id + "]: " + fullURL);

				String xpathButton = "/html/body/table/tbody/tr[2]/td/div/div/div/div[2]/div[2]/div[1]/div[1]/div/a";
				gotoURLSearchPage(driver, fullURL, xpathButton);
				WebElement btnLink = driver.findElement(By.xpath(xpathButton));
				btnLink.click();

				log4j.info("Getting : " + fullURL + " [" + id + "]");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * int tried = 0; boolean arrived = false; while (!arrived) { try {
				 * Thread.sleep(500); } catch (InterruptedException e) { // TODO Auto-generated
				 * catch block e.printStackTrace(); }
				 *
				 * String currURL = driver.getCurrentUrl(); if (currURL.equals(downloadURL)) {
				 * arrived = true; }
				 *
				 * if (tried > 20) { log4j.error("Could not load the page in " + tried +
				 * " tries, exitting !!"); System.exit(-1); } tried++; }
				 */

				count++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int trials = 0;
		int TRIAL_LIMIT = 8;
		int waitTime = 100;

		boolean completed = false;

		ArrayList<File> fileList = null;
		while (trials < TRIAL_LIMIT) {
			fileList = GlobalUtils.checkDirectory(downloadDirName, EXTENSION_LIST);
			if (fileList.size() == POOL_SIZE) {
				completed = true;
				log4j.info("All downloads completed in this cycle ..");
				break;
			} else {
				waitTime = waitTime * 2;
				try {
					log4j.info("Waiting for another " + waitTime + "(ms) slot for completion of downloads [" + trials
							+ "/" + TRIAL_LIMIT + "]");
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			trials++;
		}

		if (completed) {

			moveFilesToDest(fileList, new File(destDirName));
			log4j.info("Cycle Completed !!");
			int colid = 1;
			for (String id : idList) {
				try {
					psUpdate2.setString(colid, "Y");
					colid++;
					psUpdate2.setString(colid, id);
					psUpdate2.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else {
			log4j.info("Error occured, download could not be completed ..");
		}
	}

	public void closeDrivers() {
		for (WebDriver driver : driverList) {
			driver.quit();
			driver.close();
		}
	}

	public void updateFilesizes() { // String filesize values are converted to integer KB number for size comparison

		Statement st = null;
		try {
			st = DBconn.createStatement();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ResultSet records = null;
		String stString = "Select * from book_links where filesize2 is null";
		try {
			log4j.info("Running : " + stString);
			records = st.executeQuery(stString);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int count = 0;
		ArrayList<String> idList = new ArrayList<>();

		try {
			WebDriver driver = new ChromeDriver();
			login(driver);
			String value = null;
			while (records.next()) {
				String id = records.getString("ID");
				String sizeStr = records.getString("FILESIZE").trim();
				boolean mbSize = sizeStr.contains("MB");
				log4j.info(String.format("ID: %s, Filesize: %s, mbSize: %s", id, sizeStr, mbSize));
				if (mbSize) {
					int locDot = sizeStr.lastIndexOf(".");
					String[] bits = sizeStr.split("\\.");
					String mbNumber = bits[0];
					String rest = bits[1];
					String kbNumber = rest.replaceAll("MB", "").trim() + "0";
					value = mbNumber + kbNumber;

				} else {
					String[] bits = sizeStr.split(" ");
					value = bits[0];
				}
				psUpdate3.setInt(1, Integer.parseInt(value));
				psUpdate3.setString(2, id);
				psUpdate3.executeUpdate();

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ZLibraryChromeFileDownloader instance = new ZLibraryChromeFileDownloader(args[0]);
		instance.updateFilesizes();
		// instance.runOneCycle();
		// instance.closeDrivers();

		// instance.scrapeBookList(); // 1
		// instance.scrapeAttributes(); // 2

		// ArrayList<File> fileList =
		// GlobalUtils.checkDirectory("C:\\Users\\ulvi\\Downloads",
		// ZLibraryChromeFileDownloader.EXTENSION_LIST);

		// File destDir = new File("S:\\Ulvi\\wordspace\\Wordlet\\_BOOKS\\Repo");
		// instance.moveFilesToDest(fileList, destDir);

		// System.out.println("classpath=" + System.getProperty("java.class.path"));
		// instance.downloadFile("2343434", "url");

	}

}
