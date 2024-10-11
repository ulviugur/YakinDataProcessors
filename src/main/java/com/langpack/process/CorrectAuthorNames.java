package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.common.GlobalUtils;
import com.langpack.common.LevensteinIndex;
import com.langpack.common.XLFileIterator;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.datachannel.XLChannel;
import com.langpack.dbprocess.XLDBInterface_1;
import com.langpack.model.LibraryBookItem;
import com.langpack.scraper.ChromeHeadless;
import com.langpack.scraper.PageParser;
import com.langpack.scraper.Qualifier;
import com.langpack.scraper.SimpleHTMLTag;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class CorrectAuthorNames {

	public static final Logger log4j = LogManager.getLogger("CorrectAuthorNames");

	XLFileIterator xlIter = null;
	File sourceFile = null;
	PageParser parser = null;

	String sheetFolderStr = null;
	String sheetName = null;

	ConfigReader cfg = null;
	ConfigReader cfgChrome = null;

	String mongoURL = "mongodb://localhost:27017";
	// MongoCollection<Document> libLinksColl = null;
	MongoCollection<Document> libBooksColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XSSFWorkbook wb = null;
	XSSFSheet sheet = null;

	ChromeHeadless myChrome = null;
	HashMap<String, String> authorsMap = new HashMap<String, String>();

	public CorrectAuthorNames(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		parser = new PageParser();
		String chromeConfigFile = cfg.getValue("ChromeHeadlessConfigFile");
		myChrome = new ChromeHeadless(chromeConfigFile);

		sheetFolderStr = cfg.getValue("SheetFolder");
		sheetName = cfg.getValue("WorksheetName");

		xlIter = new XLFileIterator(new File(sheetFolderStr));
		sourceFile = xlIter.getNextImportFile();

		log4j.info("New source file opened : {}", sourceFile.getAbsolutePath());

		wb = xlIter.openWorkbook();
		sheet = xlIter.getSheet(sheetName);

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");

		libBooksColl = database.getCollection("librarybooks");

		loadCurrentAuthors();
		log4j.info("Loaded {} currently found author mappings ..", authorsMap.size());
		log4j.info("");
	}

	public void loadCurrentAuthors() {
		try {
			int lineCount = 1;
			XSSFRow rowObject = null;

			while (true) {
				log4j.info("Line [{}]", lineCount);
				rowObject = (XSSFRow) sheet.getRow(lineCount);
				if (rowObject == null) {
					break;
				}

				Cell cellStrippedAuthors = rowObject.getCell(2);
				String strippedAuthors = GlobalUtils.getCellContentAsString(cellStrippedAuthors);

				Cell cellVerifiedAuthorsOld = rowObject.getCell(3);
				String currAuthorValue = GlobalUtils.getCellContentAsString(cellVerifiedAuthorsOld);
				if (currAuthorValue != null && !"".equals(currAuthorValue)) {
					authorsMap.put(strippedAuthors, currAuthorValue);
				}

				lineCount++;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runSourceFile() {
		XSSFCellStyle styleOK = wb.createCellStyle();
		styleOK.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		styleOK.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		XSSFCellStyle styleChanged = wb.createCellStyle();
		styleChanged.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		styleChanged.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		XSSFCellStyle styleUnknown = wb.createCellStyle();
		styleUnknown.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		styleUnknown.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		try {
			int lineCount = 1;
			int URLCall = 0;
			int MAX_URL_CALL = 10000;
			XSSFRow rowObject = null;

			while (true) {

				rowObject = (XSSFRow) sheet.getRow(lineCount);
				if (rowObject == null) {
					break;
				}
				Cell cellFilename = rowObject.getCell(0);
				String filename = GlobalUtils.getCellContentAsString(cellFilename);

				Cell cellNewFilename = rowObject.getCell(1);
				String newFilename = GlobalUtils.getCellContentAsString(cellNewFilename);

				Cell cellStrippedAuthors = rowObject.getCell(2);
				String strippedAuthors = GlobalUtils.getCellContentAsString(cellStrippedAuthors);

				Cell cellVerifiedAuthorsOld = rowObject.getCell(3);
				String oldAuthorValue = GlobalUtils.getCellContentAsString(cellVerifiedAuthorsOld);
				if (oldAuthorValue != null && !"".equals(oldAuthorValue)) {
					lineCount++;
					log4j.info("Line [{}] has already a value in sheet : {} ", lineCount, oldAuthorValue);
					continue;
				}
				Cell cellVerifiedAuthorsNew = rowObject.createCell(3);
				String knownAuthor = authorsMap.get(strippedAuthors); // checking if we know this author already from
																		// previous records
				// String result = validateAuthorOnDNR(strippedAuthors);

				String result = null;
				boolean nonScrapedItem = false;
				if (knownAuthor != null) {
					log4j.info("Author {} is already resolved as : {}]", strippedAuthors, knownAuthor);
					result = knownAuthor;
					nonScrapedItem = true;
				} else {
					result = validateAuthorOn1000Kitap(strippedAuthors);
					log4j.info("Author {} is resolved as : {}]", strippedAuthors, result);
					if (result != null) { // if there is an accepted result
						authorsMap.put(strippedAuthors, result); // add the map to the Known Authors Directory
					}
					URLCall++;
				}

				if (result == null) {
					cellVerifiedAuthorsNew.setCellStyle(styleUnknown);
				} else {

					cellVerifiedAuthorsNew.setCellValue(result);
					if (strippedAuthors.equals(result)) {
						cellVerifiedAuthorsNew.setCellStyle(styleOK);
					} else {
						cellVerifiedAuthorsNew.setCellValue(result);
					}
				}
				if (!nonScrapedItem) {
					Thread.sleep(10000);
				}
				if (MAX_URL_CALL <= URLCall) {
					break;
				}
				lineCount++;
			}

		} catch (

		Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		xlIter.saveFile();
		log4j.info("Process completed ..");
	}

	private String validateAuthorOnDNR(String author) {
		author = author.replace(",", "");
		author = author.replace("(", "");
		author = author.replace(")", "");
		String page = null;
		String callURL = String.format("https://www.dr.com.tr/search?q=%s", author);
		log4j.info(callURL);
		try {
			page = GlobalUtils.callWebsite(callURL);

			if (page == null) {
				return null;
			}
			String qStringBlock = "js-facet-list-persons";
			int count = 0;
			while (true) {
				Qualifier authorQualifier = new Qualifier(qStringBlock, count, "<div", "/div>", Qualifier.GO_BACKWARD);
				String authorsDiv = parser.findBlock(page, authorQualifier);
				if (authorsDiv != null) {
					org.jsoup.nodes.Document doc = Jsoup.parse(authorsDiv);
					Elements authorSpans = doc.select("span.facet__checkbox-text");
					for (Element span : authorSpans) {
						String tmpAuthor = span.text();
						String result = checkAuthorName(author, tmpAuthor);
						if (result != null)
							return result;
					}
				} else {
					return null;
				}
				count++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public String validateAuthorOn1000Kitap(String author) {
		if (author == null || "".equals(author)) { return null;}
		author = author.replace(",", "");
		author = author.replace("(", "");
		author = author.replace(")", "");
		String page = null;
		String authorStr = author.replace(" ", "+");
		String callURL = String.format("https://1000kitap.com/ara?q=%s&bolum=yazarlar&hl=tr", authorStr);

		try {
			page = myChrome.getURLContent(callURL);
			if (page == null) {
				return null;
			}
			org.jsoup.nodes.Document doc = Jsoup.parse(page);

			Elements authorSpans = doc.select("span.text.font-bold.truncate.text-16.w-full");
			for (Element span : authorSpans) {
				String tmpAuthor = span.text();
				String result = checkAuthorName(author, tmpAuthor);
				if (result != null)
					return result;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private String checkAuthorName(String _oriAuthor, String _foundAuthor) {
		String retval = null;
		
		String cleanOriAuthor = _oriAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");
		String cleanFoundAuthor = _foundAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");

		// BOF - CASE1
		String oriAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanOriAuthor);
		String foundAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanFoundAuthor);

		String[] partsOriAuthor = oriAuthor.split(" ");
		String[] partsFoundAuthor = foundAuthor.split(" ");

		Set<String> setOriAuthor = new TreeSet<>(Arrays.asList(partsOriAuthor));
		Set<String> setFoundAuthor = new TreeSet<>(Arrays.asList(partsFoundAuthor));

		int matchCount = matchingKeys(setOriAuthor, setFoundAuthor);

		if (matchCount >= 2) {
			retval = _foundAuthor;
			// EOF - CASE1
		} else {
			// BOF - CASE2
			int dist = LevensteinIndex.distance(GlobalUtils.convertArraytoString(setOriAuthor), GlobalUtils.convertArraytoString(setFoundAuthor));
			if (dist <= partsFoundAuthor.length) {
				retval = _foundAuthor;
			}
			// EOF - CASE2
		}

		return retval;
	}

	private int matchingKeys(Set<String> original, Set<String> found) {
		// Count how many words from the original set are found in the found set
		int matchCount = 0;
		for (String word : original) {
			if (found.contains(word)) {
				matchCount++;
			}
		}

		// Calculate and return the percentage of words that matched
		return matchCount;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		CorrectAuthorNames instance;
		try {
			instance = new CorrectAuthorNames(args[0]);
			//String tmp = instance.validateAuthorOn1000Kitap("Cemal Süreyya");
			instance.runSourceFile();
			log4j.info("");
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
