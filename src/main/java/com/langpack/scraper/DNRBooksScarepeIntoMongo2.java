package com.langpack.scraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.datachannel.XLChannel;
import com.langpack.dbprocess.XLDBInterface_1;
import com.langpack.model.LibraryBookItem;
import com.langpack.model.LibraryLinkItem;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DNRBooksScarepeIntoMongo2 {

	public static final Logger log4j = LogManager.getLogger("DNRBooksLookup");

	XLDBInterface_1 xlInterface = null;

	Boolean importData = false;
	Boolean pageFound = false;
	PageParser parser = null;
	String sourceFileStr = null;

	File sourceFile = null;

	DataChannel sourceData = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String baseURL = "https://www.dr.com.tr";

	String mongoURL = "mongodb://localhost:27017";
	MongoCollection<Document> libLinksColl = null;
	MongoCollection<Document> libBooksColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLChannel xlObject = null;
	XSSFWorkbook wb = null;
			
	public DNRBooksScarepeIntoMongo2(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);

		importData = Boolean.parseBoolean(cfg.getValue("ImportData"));
		parser = new PageParser();

		sourceData = DataChannelFactory.getChannelByName("XLFile");
		xlObject = (XLChannel) sourceData;
		
		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		libLinksColl = database.getCollection("librarylinks");
		libBooksColl = database.getCollection("librarybooks");
		
		wb = xlObject.getWorkbookObject();

		boolean retval = saveSheet();
		if (!retval) {
			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");
			System.exit(-1);
		}
	}
	
	public void runSheet() {
		int runonly = 4000;
		try {
			XSSFRow rowObject = (XSSFRow) sourceData.getNextRow(); // skip title row
			while (true) {
				rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}
				Cell cellBookName = rowObject.getCell(2);
				String bookName = GlobalUtils.getCellContentAsString(cellBookName);

				Cell cellAuthor = rowObject.getCell(3);
				String author = GlobalUtils.getCellContentAsString(cellAuthor);
				
				Cell cellProcessed = rowObject.getCell(4);
				String processed = GlobalUtils.getCellContentAsString(cellProcessed);
								
				if ("Y".equals(processed)) {
					log4j.info("Skipping {}#{} as it was already processed .. ", bookName, author);
					continue;
				}
				cellProcessed = rowObject.createCell(4);
				Cell cellFoundBook = rowObject.createCell(5);
				ArrayList<String> links = collectBookLinks(bookName, author);
				String foundBook = "N";
				for (int index = 0; index < links.size(); index++) {
					String scrapeURL = links.get(index);
					
					String tmp = scrapeBookData(scrapeURL, bookName, author, index);
					if (tmp != null && "N".equals(foundBook)) {
						foundBook = "Y";
						cellFoundBook.setCellValue("Y"); // mark it as found only once
					}
				}
				
				cellProcessed.setCellValue("Y");
				cellFoundBook.setCellValue(foundBook);
				saveSheet();

				runonly--;
				if (runonly == 0) {
					break;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// Close the workbook
			try {
				wb.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log4j.info("Process completed ..");
	}

	public String scrapeBookData(String scrapeURL, String bookName, String author, Integer index) {
		String retval = null;
		String keysCombi = bookName + "#" + author;
		String page = null;
		try {
			page = GlobalUtils.callWebsite(scrapeURL);
			if (page == null) {
				return retval;
			}
		} catch (Exception ex) {
			return retval;
		}
		pageFound = true;

		if (pageFound) {
			String qStringImage = String.format("<img width=\"600px\"");

			Qualifier imageQualifier = new Qualifier(qStringImage, 0, "src=", " ", Qualifier.GO_FORWARD);
			String linkImage = parser.findBlock(page, imageQualifier);
			if (linkImage == null) {
				log4j.error("Image link {} is not found !");
				return retval;
			} else {
				log4j.info("LinkImage : " + linkImage);
				linkImage = linkImage.replace("src=", "").replace("\"", "");
			}

			String qStringData = String.format("<div class=\"row prd-detail-body\"");

			Qualifier dataQualifier = new Qualifier(qStringData, 0, "data-gtm=", ">", Qualifier.GO_FORWARD);
			String stringData = parser.findBlock(page, dataQualifier);
			if (stringData == null) {
				log4j.error("Data block not found !");
				return retval;
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

				if (bookNameVerified.equals(bookName)) {
					retval = bookNameVerified;
				}
				
				Document newDocument = convertToDocument(bookItem);
				libBooksColl.insertOne(newDocument);

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public ArrayList<String> collectBookLinks(String bookName, String author) {
		ArrayList<String> links = new ArrayList<String>();
		String keysCombi = bookName + "#" + author;
		String fullURL = String.format("%s/search?q=%s&Person[0]=%s", baseURL, bookName, author);
		log4j.info(fullURL);
		String page = null;
		try {
			page = GlobalUtils.callWebsite(fullURL);
			if (page == null) {
				return links;
			}
		} catch (Exception ex) {
			return links;
		}
		pageFound = true;

		if (pageFound) {
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

			Document newDocument = convertToDocument(item);

			libLinksColl.insertOne(newDocument);

		}
		return links;
	}

	public org.bson.Document convertToDocument(LibraryLinkItem data) {
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

	public org.bson.Document convertToDocument(LibraryBookItem data) {
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

	public boolean saveSheet() {
		FileOutputStream fileOut;
		try {
			log4j.info("Saving file {}", sourceFileStr);
			fileOut = new FileOutputStream(sourceFileStr);
			wb.write(fileOut);
			fileOut.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		DNRBooksScarepeIntoMongo2 instance;
		try {
			instance = new DNRBooksScarepeIntoMongo2(args[0]);
			// instance.collectBookLinks(); // STEP 1
			instance.runSheet(); // STEP 2
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
