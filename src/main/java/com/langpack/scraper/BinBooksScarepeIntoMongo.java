package com.langpack.scraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class BinBooksScarepeIntoMongo {

	public static final Logger log4j = LogManager.getLogger("BinBooksScarepeIntoMongo");

	XLDBInterface_1 xlInterface = null;

	Boolean importData = false;
	Boolean pageFound = false;
	PageParser parser = null;
	String sourceFileStr = null;

	File sourceFile = null;

	DataChannel sourceData = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String baseURL = "https://1000kitap.com";

	String mongoURL = "mongodb://localhost:27017";
	MongoCollection<Document> libLinksColl = null;
	MongoCollection<Document> libBooksColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLChannel xlObject = null;
	XSSFWorkbook wb = null;

	ChromeHeadless myChrome = null;

	public BinBooksScarepeIntoMongo(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);

		importData = Boolean.parseBoolean(cfg.getValue("ImportData"));
		parser = new PageParser();

		String chromeConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		myChrome = new ChromeHeadless(chromeConfigFile);

		sourceData = DataChannelFactory.getChannelByName("XLFile");
		xlObject = (XLChannel) sourceData;

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		libLinksColl = database.getCollection("BinLinks");
		libBooksColl = database.getCollection("BinBooks");

		wb = xlObject.getWorkbookObject();

		boolean retval = saveSheet();
		if (!retval) {
			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");
			System.exit(-1);
		}
	}

	public void runSheet() {
		int runonly = 1000;
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

				Cell cellFoundBook = rowObject.getCell(5);
				String foundBook = GlobalUtils.getCellContentAsString(cellFoundBook);

				if (processed == null || "".equals(processed)) {
					foundBook = "N";
					log4j.info("[{}] Processing {}#{} . ", runonly, bookName, author);
				} else {
					log4j.info("[{}] Skipping {}#{} as it was already processed .. ", runonly, bookName, author);
					continue;
				}

				cellProcessed = rowObject.createCell(4);
				cellFoundBook = rowObject.createCell(5);
				ArrayList<String> links = collectBookLinksOnBinBooks(bookName, author);

				for (int index = 0; index < links.size(); index++) {
					String scrapeURL = links.get(index);

					String tmp = scrapeBookData(scrapeURL, bookName, author, index); // there was a matching book name with minimal tolerance
					if (tmp != null) {
						foundBook = "Y";
					}
					
					try {
						Thread.sleep(1400);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
			page = myChrome.getURLContent(scrapeURL);
			if (page == null) {
				return retval;
			}
		} catch (Exception ex) {
			return retval;
		}
		pageFound = true;

		if (pageFound) {
			String linkImage = null;
			String qStringButton = String.format("dr whk-15 overflow-hidden cursor");

			Qualifier buttonQualifier = new Qualifier(qStringButton, 0, "<button", "</button>", Qualifier.GO_BACKWARD);
			String button = parser.findBlock(page, buttonQualifier);
			if (button == null) {
				log4j.error("Image button is not found !");
				return retval;
			} else {
				Qualifier imageQualifier = new Qualifier("src", 0, "<img", ">", Qualifier.GO_BACKWARD);
				String image = parser.findBlock(button, imageQualifier);

				org.jsoup.nodes.Document imgdoc = Jsoup.parse(image);
				Element imgElement = imgdoc.selectFirst("img");
				linkImage = imgElement.attr("src");
			}
			org.jsoup.nodes.Document doc = Jsoup.parse(page);
			Element dataDiv = doc.selectFirst("div.dr.flex-row.flex-wrap.gap-1_5");
			Element divBookName = doc.selectFirst("h1.text.font-bold.truncate.text-20");

			if (dataDiv == null) {
				log4j.error("Data block not found at URL : {}!", scrapeURL);
				return retval;
			} else {
				Element divAuthor = dataDiv.selectFirst("span.text-alt.text-mavi.text-14.ml-1_5");

				Elements divElements = dataDiv.select("span.text.text-14.mr-3");

				org.bson.Document attrDoc = new org.bson.Document();
				for (Iterator<Element> iterator = divElements.iterator(); iterator.hasNext();) {
					Element element = (Element) iterator.next();
					Elements attrElements = element.select(".text-alt");
					Element label = attrElements.get(0);
					String cleanLabel = label.text().replace(":", "");
					Element value = attrElements.get(1);
					attrDoc.append(cleanLabel, value.text());

				}

				String jsonData = attrDoc.toJson();
				String publisherVerified = attrDoc.getString("Publisher");
				String publishYearVerified = attrDoc.getString("First Publication Date");
				String authorVerified = divAuthor.text();
				String bookNameVerified = divBookName.text();

				LibraryBookItem bookItem = new LibraryBookItem();
				bookItem.setMetaData(jsonData); // OK
				bookItem.setAuthor(authorVerified); // OK
				bookItem.setBookName(bookNameVerified);
				bookItem.setKeysCombi(keysCombi); // OK
				bookItem.setPublisher(publisherVerified); // OK
				bookItem.setPublishYear(publishYearVerified); // OK
				bookItem.setIndex(index); // OK
				bookItem.setScrapeURL(scrapeURL); // OK
				bookItem.setThumbnailURL(linkImage); // OK

				String[] partsBook = bookName.split(" ");
				int dist = LevensteinIndex.distance(bookNameVerified, bookName);
				if (dist <= partsBook.length) {
					retval = bookNameVerified;
				}

				Document newDocument = convertToDocument(bookItem);
				libBooksColl.insertOne(newDocument);

				try {
					Thread.sleep(1000);
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

	public ArrayList<String> collectBookLinksOnBinBooks(String bookName, String author) {
		ArrayList<String> links = new ArrayList<String>();
		String keysCombi = bookName + "#" + author;
		String fullURL = String.format("%s/ara?q=%s&bolum=kitaplar", baseURL, bookName, author);
		log4j.info(fullURL);
		String page = null;
		try {
			page = myChrome.getURLContent(fullURL);
			if (page == null) {
				return links;
			}
		} catch (Exception ex) {
			return links;
		}
		pageFound = true;

		if (pageFound) {
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

		BinBooksScarepeIntoMongo instance;
		try {
			instance = new BinBooksScarepeIntoMongo(args[0]);
			// instance.collectBookLinks(); // STEP 1
			instance.runSheet(); // STEP 2
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
