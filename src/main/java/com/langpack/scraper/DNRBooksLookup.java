package com.langpack.scraper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.langpack.common.BasicClass;
import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.datachannel.XLChannel;
import com.langpack.dbprocess.XLDBInterface_1;

public class DNRBooksLookup {

	public static final Logger log4j = LogManager.getLogger("DNRBooksLookup");

	XLDBInterface_1 xlInterface = null;
	String fullURL = null;
	Boolean importData = false;
	Boolean pageFound = false;
	PageParser parser = null;
	String sourceFileStr = null;

	File sourceFile = null;

	DataChannel sourceData = null;

	protected ConfigReader cfg = null;

	private static String TITLE_REGEX = "(\\d{5})_(.+?)_\\((.+)\\)\\.([a-z]+)";

	ArrayList<String> bookKeys = new ArrayList<String>();

	String searchURL = "https://www.dr.com.tr";

	public DNRBooksLookup(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);

		importData = Boolean.parseBoolean(cfg.getValue("ImportData"));
		parser = new PageParser();

		sourceData = DataChannelFactory.getChannelByName("XLFile");

	}

	public void loadNames() {
		try {
			XSSFRow rowObject = (XSSFRow) sourceData.getNextRow(); // skip title row
			int lineCount = 2;
			while (true) {
				rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}
				Cell cellBookName = rowObject.getCell(2);
				String bookName = GlobalUtils.getCellContentAsString(cellBookName);

				Cell cellAuthor = rowObject.getCell(3);
				String author = GlobalUtils.getCellContentAsString(cellAuthor);

				bookKeys.add(bookName + " " + author);
				lineCount++;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sortXLData() {
		try {
			int lineCount = 1;
			while (true) {
				XSSFRow rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}

				Cell cellBookName = rowObject.getCell(0);

				String bookName = GlobalUtils.getCellContentAsString(cellBookName);
				log4j.info(">>>" + bookName);

				ArrayList<String> parts = parseTitle(bookName);

				String title = parts.get(0);
				String title2 = title.replace("_", " ");
				String title3 = GlobalUtils.toCamelCase(title2);

				String author = parts.get(1);
				String author2 = author.replace("_", " ");
				String author3 = GlobalUtils.toCamelCase(author2);

				Cell cellTitle = rowObject.createCell(3, CellType.STRING);
				Cell cellAuthor = rowObject.createCell(4, CellType.STRING);

				cellTitle.setCellValue(title);
				cellAuthor.setCellValue(author3);

				log4j.info("[{}] {} => {}", lineCount, title3, author3);
				lineCount++;
			}
			saveSheet();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveSheet() {

		XLChannel xlObject = (XLChannel) sourceData;
		XSSFWorkbook wb = xlObject.getWorkbookObject();

		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(sourceFileStr);

			// Write the workbook to the output stream
			wb.write(fileOut);

			// Close the file output stream
			fileOut.close();

			// Close the workbook
			wb.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public ArrayList<String> parseTitle(String fileName) {

		ArrayList<String> retval = new ArrayList<String>();

		Pattern pattern = Pattern.compile(TITLE_REGEX);

		// Match the pattern with the filename
		Matcher matcher = pattern.matcher(fileName);

		if (matcher.matches()) {
			String id = matcher.group(1); // 90149
			String title = matcher.group(2); // Ölü_Albayın_Kızları
			String author = matcher.group(3); // mansfield_katherine
			String extension = matcher.group(4); // stc

			// retval.add(id);
			retval.add(title);
			retval.add(author);

		} else {
			System.out.println("Filename does not match the expected format.");
		}
		return retval;
	}

	public void scrapeContent() {

		// Iterator<String> iter = bookKeys.iterator();

		// while (iter.hasNext()) {
		String keys = "Hayvan Çiftliği";
		fullURL = String.format("%s/search?q=%s", searchURL, keys);

		String page = GlobalUtils.callWebsite(fullURL);
		pageFound = true;
		if (pageFound) {
			String key = "<h3 class=\"seo-heading\"><a href=\"/kitap/";


			for (int jumper = 0; jumper < 5; jumper++) {
				Qualifier pageQualifier = new Qualifier(key, jumper, "<a href=", ">", Qualifier.GO_BACKWARD);

				String block = parser.findBlock(page, pageQualifier);
				if (block == null) {
					log4j.error("Link is not found !");
				} else {
					log4j.info("Block : {}", block);
				}
			}
		}
		System.exit(-1);
		// }

	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		DNRBooksLookup instance;
		try {
			instance = new DNRBooksLookup(args[0]);
			// instance.loadNames();
			instance.scrapeContent();
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
