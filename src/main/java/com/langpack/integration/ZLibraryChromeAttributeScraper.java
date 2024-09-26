package com.langpack.integration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.langpack.common.BasicClass;
import com.langpack.common.ConfigReader;
import com.langpack.structure.WordGenerator;

public class ZLibraryChromeAttributeScraper extends BasicClass {

	WebDriver driver = null;
	String chromeDriverLocation = null;
	String chromeDriverArguments = null;
	String baseURL = null;
	String loginURL = null;

	String _EXTENSION = null;
	String _KEYWORD = "";
	int _YEAR;

	TreeSet<String> urlSet = new TreeSet<>();

	public ZLibraryChromeAttributeScraper(String cfgFileName) {
		super(cfgFileName);

		String chromeHeadlessConfigFile = cfg.getValue("ChromeHeadlessConfigFile");

		ConfigReader chromeCfg = new ConfigReader(chromeHeadlessConfigFile);

		baseURL = cfg.getValue("Download.Site");
		loginURL = cfg.getValue("Login.Site");

		chromeDriverLocation = chromeCfg.getValue("chromeDriverLocation");
		chromeDriverArguments = chromeCfg.getValue("chromeDriverArguments");

		// Setting system properties of ChromeDriver
		System.setProperty("webdriver.chrome.driver", chromeDriverLocation);

		// ChromeOptions options = new ChromeOptions();
		// options.addArguments("--headless", "--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");
		// String[] tmpArguments = chromeDriverArguments.split(";");
		// options.addArguments(tmpArguments);
		// options.addArguments("--disable-gpu",
		// "--window-size=1920,1200","--ignore-certificate-errors");

		// driver = new ChromeDriver(options);
		driver = new ChromeDriver();

		// Creating an object of ChromeDriver driver = new ChromeDriver();
		driver.manage().window().maximize();
		// downloader = new
		// ZLibraryDownloader("S:\\Ulvi\\wordspace\\Wordlet\\config\\ZLibrary_Downloader_1.cfg");

	}

	public void login(WebDriver _driver) {

		_driver.get(loginURL);

		WebElement elementName = _driver.findElement(By.name("email"));
		elementName.sendKeys("ulvi.ugur@gmail.com");

		WebElement elementPassword = _driver.findElement(By.name("password"));
		elementPassword.sendKeys("Reuters50$");

		WebElement elementSubmit = _driver.findElement(By.name("submit"));
		elementSubmit.click();

		log4j.info("LoggedIn");

	}

	private void gotoURLSearchPage(String destURL, String markerString) { // markerString is an indicative phrase which
																			// confirms that the page is loaded
		try {
			driver.get(destURL);
			Thread.sleep(2000);
			boolean displayed = false;
			// Thread.sleep(1200);
			int tried = 0;
			while (!displayed) {
				displayed = driver.findElement(By.className(markerString)).isDisplayed();
				tried++;
				Thread.sleep(500);
				if (tried > 20) {
					log4j.error("Could not load the page in " + tried + " tries, exitting !!");
					System.exit(-1);
				}
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void gotoURLLevel2(String destURL, String markerString) {
		try {

			Thread.sleep(1200);
			driver.get(destURL);
			boolean displayed = false;
			// Thread.sleep(1200);
			int tried = 0;
			while (!displayed) {
				displayed = driver.findElement(By.id(markerString)).isDisplayed();
				tried++;
				Thread.sleep(500);
				if (tried > 20) {
					log4j.error("Could not load the page in " + tried + " tries, exitting !!");
					System.exit(-1);
				}
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void scrapeBookListInYear() {

		String url = makeURL();
		gotoURLSearchPage(url, "subprojectsSearch");

		List<WebElement> links = driver.findElements(By.tagName("a"));

		int noPages = 0;
		String pageBaseLink = null;
		for (WebElement link : links) {
			String role = link.getAriaRole();
			if (role.equals("link")) {
				String target = link.getAttribute("href");
				if (target.contains("&page=")) {
					log4j.info(target);
					String[] pageLink = target.split("&page=");
					pageBaseLink = pageLink[0];
					int pageNum = Integer.parseInt(pageLink[1]);
					if (pageNum > noPages) {
						noPages = pageNum;
					}
				}
			}

		}

		if (noPages >= 10) {
			log4j.fatal(
					"We have 10 pages items for the year : " + _YEAR + ", will use artificial keywords for search ..");
			WordGenerator gen = new WordGenerator(2);
			String keyNew = gen.nextWord();
			do {
				try {
					_KEYWORD = keyNew.trim();
					url = makeURL();
					gotoURLSearchPage(url, "subprojectsSearch");
					iterateThroughPages(url, noPages);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} while ((keyNew = gen.nextWord()) != null);
			_KEYWORD = "";
		} else {
			iterateThroughPages(url, noPages);
		}

	}

	private void iterateThroughPages(String pageBaseLink, int noPages) {
		List<WebElement> links = null;
		String tmpURL = pageBaseLink;
		int page = 1;

		do {

			try {
				Thread.sleep(1200);
				driver.get(tmpURL);
				Thread.sleep(1200);
				String source = driver.getPageSource();
				if (source.contains("On your request nothing has been found")) {
					break;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			links = driver.findElements(By.tagName("a"));
			for (WebElement link : links) {
				String role = link.getAriaRole();
				if (role.equals("link")) {
					String target = link.getAttribute("href");
					if (target.contains("book4you.org/book/")) {
						try {
							if (urlSet.contains(target)) {
								log4j.info("Already collected : " + target + " , skipping ..");
							} else {
								psInsert1.setString(1, target);
								psInsert1.setString(2, Integer.toString(_YEAR));
								psInsert1.setString(3, _EXTENSION);

								psInsert1.execute();
								urlSet.add(target);
							}
						} catch (java.sql.SQLIntegrityConstraintViolationException e) {
							// TODO Auto-generated catch block
							// log4j.info("");
							e.printStackTrace();
						} catch (SQLException e) {
							e.printStackTrace();
						}

					}
				}
			}

			page++;
			tmpURL = pageBaseLink + "&page=" + page;
		} while (true);

	}

	public void scrapeBookList() {
		// String[] EXTENSION_ARRAY = new String[] {"pdf", "epub", "djvu", "fb2", "txt",
		// "rar", "mobi", "lit", "doc", "rtf", "azw3"};
		String[] EXTENSION_ARRAY = new String[] { "pdf", "epub" };

		for (String element : EXTENSION_ARRAY) {
			_EXTENSION = element;

			int yearStart = 2007;

			Date currTime = new Date();
			// _YEAR = currTime.getYear()+1900;
			int currentYear = 2007;// currTime.getYear()+1900;

			_YEAR = yearStart;
			while (_YEAR <= currentYear) {
				scrapeBookListInYear();
				_YEAR = _YEAR + 1;
			}
		}

	}

	private String makeURL() {
		String url2 = String.format(baseURL, _YEAR, _YEAR);
		String url1 = url2.replace("_FILLER_", "%5B%5D");
		String url0 = url1.replace("_EXT_", _EXTENSION);
		String url = url0.replace("_KEYWORD_", _KEYWORD);
		return url;
	}

	public void scrapeAttributes() {

		ResultSet records = null;
		try {
			log4j.info("Running : " + selectString1);
			records = psSelect1.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int recordCount = 1;
		HashMap<String, String> attributes = new HashMap<>();
		attributes.put("Categories", "");
		try {
			while (records.next()) {
				String id = records.getString("ID");
				String url = records.getString("FULLURL");
				log4j.info("[ " + recordCount + "/" + id + "] URL : " + url);

				gotoURLSearchPage(url, "btn-group");

				String coverpage_url = null;

				WebElement coverElement = driver.findElement(By.className("details-book-cover-content"));
				try {
					WebElement imgElement = coverElement.findElement(By.tagName("img"));
					if (imgElement != null) {
						coverpage_url = imgElement.getDomAttribute("src");
					}
				} catch (org.openqa.selenium.NoSuchElementException ex) {

				}

				WebElement bookDetailsBox = driver.findElement(By.className("bookDetailsBox"));

				List<WebElement> children = bookDetailsBox.findElements(By.xpath("./child::*"));
				for (WebElement tiny : children) {
					String tmp = tiny.getText();
					String[] values = tiny.getText().split("\\n");
					if (values.length == 2) {
						String label = values[0];
						String value = values[1];
						attributes.put(label.replaceAll(":", ""), value);
					}
				}
				StringBuilder sb = new StringBuilder();
				if (attributes != null) {
					for (Object key : attributes.keySet()) {
						Object value = attributes.get(key);
						sb.append("{");
						sb.append(key);
						sb.append(",");
						sb.append(value);
						sb.append("}");
					}
				}
				// String retval = "[" + sb.toString() + "]";

				WebElement topElement = driver.findElement(By.className("col-sm-9"));
				WebElement titleTag = topElement.findElement(By.tagName("h1"));
				String bookname = titleTag.getText();

				String authors = "";
				List<WebElement> authorLinks = topElement.findElements(By.tagName("a"));
				for (WebElement alink : authorLinks) {
					authors = authors.concat(alink.getText());
					// log4j.info(authors);
				}

				WebElement btnGroup = driver.findElement(By.className("btn-group"));
				List<WebElement> buttonElements = btnGroup.findElements(By.tagName("a"));
				String download_url = null;
				for (WebElement buttonElement : buttonElements) {
					String className = buttonElement.getDomAttribute("class");
					if ("btn btn-primary dlButton addDownloadedBook".equals(className)) {
						download_url = buttonElement.getDomAttribute("href");
					}
					// log4j.info(authors);
				}

				try {
					WebElement moreBtn = driver.findElement(By.className("moreBtn"));
					if (moreBtn != null) {
						moreBtn.click();
					}
				} catch (org.openqa.selenium.NoSuchElementException ex) {

				}

				String synopsis = "";
				try {
					WebElement bookDescriptionBox = driver.findElement(By.id("bookDescriptionBox"));
					if (bookDescriptionBox != null) {
						synopsis = bookDescriptionBox.getText();
					}
				} catch (org.openqa.selenium.NoSuchElementException ex) {

				}
				if (synopsis.length() > 3000) {
					synopsis = synopsis.substring(0, 2700) + " ...";
				}

				String fileAttributes = attributes.get("File");
				String[] fields = fileAttributes.split(",");
				String contentType = fields[0];
				String fileSize = fields[1];

				int colid = 1;
				psUpdate1.setString(colid, download_url);
				colid++;
				psUpdate1.setString(colid, bookname);
				colid++;
				psUpdate1.setString(colid, authors);
				colid++;

				psUpdate1.setString(colid, coverpage_url);
				// colid++;
				// psUpdate1.setString(colid, synopsis);
				colid++;
				psUpdate1.setString(colid, attributes.get("Categories").replaceAll("Add a category", ""));
				colid++;
				psUpdate1.setString(colid, attributes.get("Year"));
				colid++;
				psUpdate1.setString(colid, attributes.get("Edition"));
				colid++;
				psUpdate1.setString(colid, attributes.get("Publisher"));
				colid++;
				psUpdate1.setString(colid, attributes.get("Language"));
				colid++;
				psUpdate1.setString(colid, attributes.get("Pages"));
				colid++;
				psUpdate1.setString(colid, attributes.get("File"));
				colid++;
				psUpdate1.setString(colid, attributes.get("ISBN 10"));
				colid++;
				psUpdate1.setString(colid, attributes.get("ISBN 13"));
				colid++;
				psUpdate1.setString(colid, contentType);
				colid++;
				psUpdate1.setString(colid, fileSize);
				colid++;
				psUpdate1.setString(colid, id);
				int updated = psUpdate1.executeUpdate();

				recordCount++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ZLibraryChromeAttributeScraper instance = new ZLibraryChromeAttributeScraper(args[0]);

		// instance.scrapeBookList(); // 1
		// instance.scrapeAttributes(); // 2

		instance.scrapeAttributes();

	}

}
