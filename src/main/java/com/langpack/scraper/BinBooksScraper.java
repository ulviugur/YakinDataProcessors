package com.langpack.scraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.LevensteinIndex;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.datachannel.XLChannel;
import com.langpack.dbprocess.XLDBInterface_1;
import com.langpack.model.LibraryBookItem;
import com.langpack.model.LibraryLinkItem;
import com.langpack.process.golddata.MergeBooksData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class BinBooksScraper implements BookScraper {

	public static final Logger log4j = LogManager.getLogger("BinBooksScraper");

	static PageParser parser = new PageParser();

	// protected ConfigReader cfg = null;
	static String baseURL = "https://1000kitap.com";

	static ChromeHeadless myChrome = new ChromeHeadless(
			"C:\\Ulvi\\superspace\\YakinDataProcessors\\config\\ChromeHeadless_1.cfg");

//	public BinBooksScraper(String cfgFileName) throws UnknownDataChannelException {
//		cfg = new ConfigReader(cfgFileName);
//	}

//	public void runSheet() {
//		int runonly = 1000;
//		try {
//			XSSFRow rowObject = (XSSFRow) sourceData.getNextRow(); // skip title row
//			while (true) {
//				rowObject = (XSSFRow) sourceData.getNextRow();
//				if (rowObject == null) {
//					break;
//				}
//				Cell cellBookName = rowObject.getCell(2);
//				String bookName = GlobalUtils.getCellContentAsString(cellBookName);
//
//				Cell cellAuthor = rowObject.getCell(3);
//				String author = GlobalUtils.getCellContentAsString(cellAuthor);
//
//				Cell cellProcessed = rowObject.getCell(4);
//				String processed = GlobalUtils.getCellContentAsString(cellProcessed);
//
//				Cell cellFoundBook = rowObject.getCell(5);
//				String foundBook = GlobalUtils.getCellContentAsString(cellFoundBook);
//
//				if (processed == null || "".equals(processed)) {
//					foundBook = "N";
//					log4j.info("[{}] Processing {}#{} . ", runonly, bookName, author);
//				} else {
//					log4j.info("[{}] Skipping {}#{} as it was already processed .. ", runonly, bookName, author);
//					continue;
//				}
//
//				cellProcessed = rowObject.createCell(4);
//				cellFoundBook = rowObject.createCell(5);
//				ArrayList<String> links = collectBookLinksOnBinBooks(bookName, author);
//
//				for (int index = 0; index < links.size(); index++) {
//					String scrapeURL = links.get(index);
//
//					String tmp = scrapeBookDataOnBinBooks(scrapeURL, bookName, author, index); // there was a matching
//																								// book name with
//																								// minimal tolerance
//					if (tmp != null) {
//						foundBook = "Y";
//					}
//
//					try {
//						Thread.sleep(1400);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//
//				cellProcessed.setCellValue("Y");
//				cellFoundBook.setCellValue(foundBook);
//				saveSheet();
//
//				runonly--;
//				if (runonly == 0) {
//					break;
//				}
//			}
//
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			// Close the workbook
//			try {
//				wb.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		log4j.info("Process completed ..");
//	}

	public Document scrapeBookData(String scrapeURL, String bookName, String author, Integer index) {
		WebDriver driver = myChrome.driver;
		
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
		org.bson.Document newDocument = null; // new org.bson.Document();
		String keysCombi = bookName + "#" + author;
		String page = null;
		try {
			page = myChrome.getURLContent(scrapeURL);
			if (page == null) {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}

        // Wait for the button containing the text to become visible
		WebElement buttonElement = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//button[.//span[text()='Bütün basım bilgilerini göster']]")));

		buttonElement.click();
		
		try {
			page = myChrome.getPageSource();
			if (page == null) {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}
		
		String linkImage = null;
		String qStringButton = String.format("dr whk-15 overflow-hidden cursor");

		Qualifier buttonQualifier = new Qualifier(qStringButton, 0, "<button", "</button>", Qualifier.GO_BACKWARD);
		String button = parser.findBlock(page, buttonQualifier);
		if (button == null) {
			log4j.error("Image button is not found !");
			return null;
		} else {
			Qualifier imageQualifier = new Qualifier("src", 0, "<img", ">", Qualifier.GO_BACKWARD);
			String image = parser.findBlock(button, imageQualifier);

			org.jsoup.nodes.Document imgdoc = Jsoup.parse(image);
			Element imgElement = imgdoc.selectFirst("img");
			linkImage = imgElement.attr("src");
		}
				
		org.jsoup.nodes.Document doc = Jsoup.parse(page);
		// Element dataDiv = doc.selectFirst("div.dr.flex-row.flex-wrap.gap-1_5");
		//Element dataDiv = doc.selectFirst("div.dr.md-\\:ph-4.pt-2.gap-3.pb-safe.overflow-hidden");
		   Element dataDiv = doc.selectFirst("div[class*='md-:ph-4'][class*='pt-2'][class*='gap-3'][class*='pb-safe'][class*='overflow-hidden']");

		Element divBookName = doc.selectFirst("h1.text.font-bold.truncate.text-20");
		Element divSubtitle = doc.selectFirst("h2.text.font-medium.text-silik.truncate.text-15");
		Element divAuthor = doc.selectFirst("a[href*='/yazar/']");

		if (dataDiv == null) {
			log4j.error("Data block not found at URL : {}!", scrapeURL);
			return null;
		} else {
			//Element divAuthor = dataDiv.selectFirst("span.text.text-mavi.text-14");

			// Elements divElements = doc.select("div.dr.flex-row.gap-2");

//			for (Iterator<Element> iterator = divElements.iterator(); iterator.hasNext();) {
//				Element element = (Element) iterator.next();
//				Elements labelElements = element.select("span.text.text-silik-v2.truncate.text-14");
//				Element label = labelElements.get(0);
//				String cleanLabel = label.text().replace(":", "");
//				
//				Elements valueElements = element.select("span.text.text-mavi.text-14");
//				Element valueSpan = valueElements.get(0);
//				String value = valueSpan.text();
//				
//				attrDoc.append(cleanLabel, value);
//			}
			
			org.bson.Document attrDoc = new org.bson.Document();
			
			Element spanElement = doc.selectFirst("span:contains(Basım Bilgileri)");
			if (spanElement != null) {
	            // Find the parent element of the <span>
	            Element parentElement = spanElement.parent().parent().parent();
				Elements divElements2 = parentElement.select("div.dr.flex-row.gap-2");
				attrDoc = getKeyValueMatrix(divElements2);
			}

			String jsonData = attrDoc.toJson();
			String publisherVerified = GlobalUtils.getBookMetadataValue(attrDoc, "Yayınevi");
			String publishYearVerified = GlobalUtils.getBookMetadataValue(attrDoc, "İlk Yayın Tarihi");
			String authorVerified = divAuthor.text();
			String bookNameVerified = divBookName.text();
			String subtitle = "";
			
			if (divSubtitle != null) { subtitle = divSubtitle.text();}

			LibraryBookItem bookItem = new LibraryBookItem();
			bookItem.setMetaData(jsonData); // OK
			bookItem.setAuthor(authorVerified); // OK
			bookItem.setBookName(bookNameVerified);
			bookItem.setSubtitle(subtitle);
			bookItem.setKeysCombi(keysCombi); // OK
			bookItem.setPublisher(publisherVerified.toString()); // OK
			bookItem.setPublishYear(publishYearVerified.toString()); // OK
			bookItem.setIndex(index); // OK
			bookItem.setScrapeURL(scrapeURL); // OK
			bookItem.setThumbnailURL(linkImage); // OK

			String combinedBookname = (bookNameVerified + " " + subtitle).trim();
			String resultBook1 = MergeBooksData.compareItems(bookNameVerified, bookName, 2);
			String resultBook2 = MergeBooksData.compareItems(combinedBookname, bookName, 2);
			String resultAuthor = MergeBooksData.compareItems(authorVerified, author, 2);
			
			if (resultAuthor != null) { // author is not wrong, good start
				if (resultBook1 != null || resultBook2 != null) {
					newDocument = convertToDocument(bookItem);
				}
			}
			
			return newDocument;
		}
	}
	private Document getKeyValueMatrix(Elements dataElements) {
		org.bson.Document retval = new org.bson.Document();
		for (Iterator<Element> iterator = dataElements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			Elements labelElements = element.select("span.text.text-silik-v2.truncate.text-14");
			if (labelElements.size()>0) {
				Element label = labelElements.get(0);
				String cleanLabel = label.text().replace(":", "");
				log4j.info("Label = {}", cleanLabel);
				
				// either with additional links (in blue)
				Elements valueElements = element.select("span.text.text-mavi.text-14");
				if ( valueElements == null || valueElements.isEmpty()) {
					// or just data elements
					valueElements = element.select("div.dr.flex-1 > span.text.text-14");
				}
				ArrayList<String> values = collectValues(valueElements);
				if (valueElements.size() > 0) {
					retval.append(cleanLabel, values);
				} else {
					log4j.info("Content for {} label is empty :/", cleanLabel);
				}
			}

		}
		return retval;
	}
	private ArrayList<String>  collectValues(Elements items) {
	    // Initialize a Document object to hold the data
	    Document result = new Document();

	    // Initialize an ArrayList to store collected values
	    ArrayList<String> valuesList = new ArrayList<>();

	    // Iterate over the items
	    for (Element valElement : items) {
	        String value = valElement.text();
	        valuesList.add(value); // Add each value to the list
	    }

	    // Add the list to the Document under a key, e.g., "values"
	    return valuesList;
	}


	public Document collectBookLinks (String bookName, String author) {
		ArrayList<String> links = new ArrayList<String>();
		String keysCombi = bookName + "#" + author;
		String fullURL = String.format("%s/ara?q=%s&bolum=kitaplar", baseURL, bookName, author);
		log4j.info(fullURL);
		String page = null;
		try {
			page = myChrome.getURLContent(fullURL);
			if (page == null) {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}

		String qString = "dr self-start max-w-full flex-row cursor";

		int jumper = 0;
		boolean foundLink = true;
		while (foundLink) {
			Qualifier pageQualifier = new Qualifier(qString, jumper, "<a", "</a>", Qualifier.GO_BACKWARD);
			String block = parser.findBlock(page, pageQualifier);
			if (block == null) {
				// log4j.error("Link is not found !");
				break;
			} else {
				org.jsoup.nodes.Document doc = Jsoup.parse(block);
				Element link = doc.selectFirst("a");

				if (link != null) {
					String subLink = link.attr("href");
					String fullLink = baseURL + subLink;
					links.add(fullLink);
					log4j.info("[{}] FullLink : {}", jumper, fullLink);
				}
			}
			jumper++;
		}
		LibraryLinkItem item = new LibraryLinkItem();
		item.setBookName(bookName);
		item.setAuthor(author);
		item.setScrapeURL(fullURL);
		item.setLinks(links);
		item.setKeysCombi(keysCombi);

		Document newDocument = convertToDocument(item);
		return newDocument;
	}

	public static org.bson.Document convertToDocument(LibraryLinkItem data) {
		org.bson.Document document = new org.bson.Document(); // Create a new BSON Document
		for (Field field : LibraryLinkItem.class.getDeclaredFields()) {
			field.setAccessible(true); // Allow access to private fields
			try {
				Object value = field.get(data);
				if (value != null) {
					document.append(field.getName(), value); // Populate the Document with fields
				}
			} catch (IllegalAccessException e) {
				System.err.println("Error accessing field: " + e.getMessage());
			}
		}
		return document; // Return the BSON Document
	}

	public static org.bson.Document convertToDocument(LibraryBookItem data) {
		org.bson.Document document = new org.bson.Document(); // Create a new BSON Document
		for (Field field : LibraryBookItem.class.getDeclaredFields()) {
			field.setAccessible(true); // Allow access to private fields
			try {
				Object value = field.get(data);
				if (value != null) {
					document.append(field.getName(), value); // Populate the Document with fields
				}
			} catch (IllegalAccessException e) {
				System.err.println("Error accessing field: " + e.getMessage());
			}
		}
		return document; // Return the BSON Document
	}

//	public boolean saveSheet() {
//		FileOutputStream fileOut;
//		try {
//			log4j.info("Saving file {}", sourceFileStr);
//			fileOut = new FileOutputStream(sourceFileStr);
//			wb.write(fileOut);
//			fileOut.close();
//			return true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
//	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		BinBooksScraper instance;
		// instance = new BinBooksScraper(args[0]);
		// instance.collectBookLinks(); // STEP 1
		// instance.runSheet(); // STEP 2

		// instance.scrapeContent();

	}

}
