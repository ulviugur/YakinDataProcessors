package com.langpack.scraper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.model.LibraryBookItem;
import com.langpack.model.LibraryLinkItem;
import com.langpack.process.golddata.MergeBooksData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DNRBooksScraper implements BookScraper {

	public static final Logger log4j = LogManager.getLogger("DNRBooksLookup");

	Boolean pageFound = false;

	String sourceFileStr = null;

	File sourceFile = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	private static String baseURL = "https://www.dr.com.tr";
	private static PageParser parser = new PageParser();

//	String mongoURL = "mongodb://localhost:27017";
//	MongoCollection<Document> libLinksColl = null;
//	MongoCollection<Document> libBooksColl = null;
//
//	MongoDatabase database = null;
//	MongoClient mongoClient = null;
//
//	XSSFWorkbook wb = null;
//	XSSFSheet sheet = null;
//	FileInputStream fsIP = null;

	Integer bookNameColumnNumber;
	Integer authorColumnNumber;
	Integer processedColumnNumber;
	Integer foundBookColumnNumber;
	Integer skipLines;

	public DNRBooksScraper() {
		// cfg = new ConfigReader(cfgFileName);

//		sourceFileStr = cfg.getValue("BooksFile");
//		sourceFile = new File(sourceFileStr);
//		String sheetName = cfg.getValue("Sheetname");
//
//		try {
//			fsIP = new FileInputStream(sourceFile);
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		// Access the workbook
//		log4j.info(String.format("Loading file %s ..", sourceFile.getAbsoluteFile()));
//		try {
//			wb = new XSSFWorkbook(fsIP);
//			sheet = wb.getSheet(sheetName);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		mongoClient = MongoClients.create();
//
//		String dbName = cfg.getValue("MongoDBName");
//
//		database = mongoClient.getDatabase(dbName);
//
//		libLinksColl = database.getCollection("librarylinks");
//		libBooksColl = database.getCollection("librarybooks");
//
//		bookNameColumnNumber = Integer.parseInt(cfg.getValue("Field.BookNameColumnNumber", "5"));
//		authorColumnNumber = Integer.parseInt(cfg.getValue("Field.AuthorColumnNumber", "6"));
//		processedColumnNumber = Integer.parseInt(cfg.getValue("Field.FoundColumnNumber", "7"));
//		foundBookColumnNumber = Integer.parseInt(cfg.getValue("Field.FoundBookColumnNumber", "8"));
//		skipLines = Integer.parseInt(cfg.getValue("SkipLines", "1"));
//
//		boolean retval = saveSheet();
//		if (!retval) {
//			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");
//			System.exit(-1);
//		}
	}

	public void runSheetOnDNR() {
//		int rowCount = skipLines;
//		try {
//			XSSFRow rowObject = null;
//			while ((rowObject = (XSSFRow) sheet.getRow(rowCount)) != null) {
//
//				Cell cellBookName = rowObject.getCell(bookNameColumnNumber);
//				String bookName = GlobalUtils.getCellContentAsString(cellBookName);
//
//				Cell cellAuthor = rowObject.getCell(authorColumnNumber);
//				String author = GlobalUtils.getCellContentAsString(cellAuthor);
//
//				Cell cellProcessed = rowObject.getCell(processedColumnNumber);
//				String processed = GlobalUtils.getCellContentAsString(cellProcessed);
//
//				if ("Y".equals(processed)) {
//					log4j.info("Skipping {}#{} as it was already processed .. ", bookName, author);
//					rowCount++;
//					continue;
//				}
//
//				cellProcessed = rowObject.createCell(processedColumnNumber);
//
//				Cell cellFoundBook = rowObject.createCell(foundBookColumnNumber);
//
//				Document links = collectBookLinksFromDNR(bookName, author);
//				String foundBook = "N";
//				for (int index = 0; index < links.size(); index++) {
//					String scrapeURL = links.get(index);
//
//					String tmp = scrapeBookDataOnDNRBooks(scrapeURL, bookName, author, index);
//					if (tmp != null && "N".equals(foundBook)) {
//						foundBook = "Y";
//						cellFoundBook.setCellValue("Y"); // mark it as found only once
//					}
//				}

//				cellProcessed.setCellValue("Y");
//				cellFoundBook.setCellValue(foundBook);
//				saveSheet();
//
//				rowCount++;
//			}
//
//		} catch (Exception e) {
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
	}

	public Document scrapeBookData(String scrapeURL, String bookName, String author, Integer index) {
		org.bson.Document newDocument = new org.bson.Document();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String keysCombi = bookName + "#" + author;
		String page = null;
		try {
			page = GlobalUtils.callWebsite(scrapeURL);
			if (page == null) {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}

		String qStringImage = String.format("<img width=\"600px\"");

		Qualifier imageQualifier = new Qualifier(qStringImage, 0, "src=", " ", Qualifier.GO_FORWARD);
		String linkImage = parser.findBlock(page, imageQualifier);
		if (linkImage == null) {
			log4j.error("Image link {} is not found !");
			return null;
		} else {
			log4j.info("LinkImage : " + linkImage);
			linkImage = linkImage.replace("src=", "").replace("\"", "");
		}

		String qStringData = String.format("<div class=\"row prd-detail-body\"");

		Qualifier dataQualifier = new Qualifier(qStringData, 0, "data-gtm=", ">", Qualifier.GO_FORWARD);
		String stringData = parser.findBlock(page, dataQualifier);
		if (stringData == null) {
			log4j.error("Data block not found !");
			return null;
		} else {
			String qStringYear = String.format("İlk Baskı Yılı:");
			Qualifier yearQualifier = new Qualifier(qStringYear, 0, "<span>", "</span>", Qualifier.GO_FORWARD);
			String stringYear = parser.findBlock(page, yearQualifier);
			SimpleHTMLTag tagYear = new SimpleHTMLTag(stringYear, true);
			String yearVerified = tagYear.getContent().trim();

			String cleanData = stringData.replace("\"", "").replace(">", "").replaceAll("data-gtm=", "");
			String jsonData = cleanData.replaceAll("&quot;", "\"");
			Document dataDocument = Document.parse(jsonData);
			String publisherVerified = (String) dataDocument.get("publisher");
			String authorVerified = (String) dataDocument.get("author");
			String bookNameVerified = (String) dataDocument.get("item_name");

			LibraryBookItem bookItem = new LibraryBookItem();
			bookItem.setMetaData(jsonData);
			bookItem.setAuthor(authorVerified);
			bookItem.setBookName(bookNameVerified);
			bookItem.setKeysCombi(keysCombi);
			bookItem.setPublisher(publisherVerified);
			bookItem.setPublishYear(yearVerified);
			bookItem.setIndex(index);
			bookItem.setScrapeURL(scrapeURL);
			bookItem.setThumbnailURL(linkImage);

			String resultBook = MergeBooksData.compareItems(bookNameVerified, bookName, 2);
			String resultAuthor = MergeBooksData.compareItems(authorVerified, author, 2);

			if (resultBook != null && resultAuthor != null) {
				newDocument = convertToDocument(bookItem);
				return newDocument;
			} else {
				return null;
			}

		}

	}

	public Document collectBookLinks(String bookName, String author) {
		org.bson.Document newDocument = new org.bson.Document();
		ArrayList<String> links = new ArrayList<String>();
		String keysCombi = bookName + "#" + author;
		String fullURL = String.format("%s/search?q=%s&Person[0]=%s", baseURL, bookName, author);
		log4j.info(fullURL);
		String page = null;
		try {
			page = GlobalUtils.callWebsite(fullURL);
			if (page == null) {
				return null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		String qString = "<h3 class=\"seo-heading\"><a href=\"/kitap/";

		int jumper = 0;
		boolean foundLink = true;
		while (foundLink) {
			Qualifier pageQualifier = new Qualifier(qString, jumper, "<a href=", ">", Qualifier.GO_BACKWARD);
			String block = parser.findBlock(page, pageQualifier);
			if (block == null) {
				// log4j.error("Link is not found !");
				break;
			} else {
				org.jsoup.nodes.Document doc = Jsoup.parse(block);
				Element link = doc.selectFirst("a.js-search-prd-item");

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

		newDocument = convertToDocument(item);

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

		DNRBooksScraper instance;

		instance = new DNRBooksScraper();
		instance.runSheetOnDNR(); // STEP 1
		// instance.runSheet(); // STEP 2

		// instance.scrapeContent();

	}

}
