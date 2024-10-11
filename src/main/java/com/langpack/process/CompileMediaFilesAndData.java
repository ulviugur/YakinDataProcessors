package com.langpack.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableParts;

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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import zemberek.core.io.Files;

public class CompileMediaFilesAndData {

	public static final Logger log4j = LogManager.getLogger("CompileMediaFiles");

	XLDBInterface_1 xlInterface = null;

	String sourceFileStr = null;
	File sourceFile = null;

	DataChannel sourceData = null;

	protected ConfigReader cfg = null;

	String mongoURL = "mongodb://localhost:27017";

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLChannel xlObject = null;
	XSSFWorkbook wb = null;

	File stcSourceDir = null;
	File stcTargetDir = null;
	
	MongoCollection<Document> goldBooksColl = null;
	MongoCollection<Document> goldLinksColl = null;
	

	boolean copyFiles = false;
	
	public CompileMediaFilesAndData(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);

		sourceData = DataChannelFactory.getChannelByName("XLFile");
		xlObject = (XLChannel) sourceData;

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		goldBooksColl = database.getCollection("GoldBooks");
		goldLinksColl = database.getCollection("GoldLinks");

		wb = xlObject.getWorkbookObject();
		
		copyFiles = Boolean.getBoolean(cfg.getValue("Process.CopyFiles", "false")); 

//		boolean retval = saveSheet();
//		if (!retval) {
//			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");
//
//			System.exit(-1);
//		}

		stcSourceDir = new File(cfg.getValue("STCSourceDirectory"));
		stcTargetDir = new File(cfg.getValue("STCTargetDirectory"));

	}

	public void runSheet() {
		try {
			XSSFRow rowObject = (XSSFRow) sourceData.getNextRow(); // skip title row
			int rowCount = 1;
			while (true) {
				rowCount++;
				rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}

				Cell cellCurrFile = rowObject.getCell(0);
				String currFileName = GlobalUtils.getCellContentAsString(cellCurrFile);

				Cell cellBookTitle = rowObject.getCell(2);
				String bookTitle = GlobalUtils.getCellContentAsString(cellBookTitle);

				Cell cellAuthor = rowObject.getCell(3);
				String author = GlobalUtils.getCellContentAsString(cellAuthor);

				File currFile = new File(stcSourceDir, currFileName);
				if (!currFile.exists()) {
					log4j.info("{} STC File {} not found !! Exitting", rowCount, currFile.getAbsolutePath());
					System.exit(-1);
				}

				Cell cellProcessed = rowObject.getCell(4);
				String processed = GlobalUtils.getCellContentAsString(cellProcessed);

				if (processed == null || "".equals(processed)) {
					log4j.info("[{}] Processing {}#{} . ", rowCount, bookTitle, author);
				} else {
					log4j.info("[{}] Skipping {}#{} as it was already processed .. ", rowCount, bookTitle, author);
					continue;
				}

				String newFilename = formNewFileName(currFileName, bookTitle, author);
				File newFile = new File(stcTargetDir, newFilename);

				if (copyFiles) {
					try {

						Files.copy(currFile, newFile);
						if (newFile.exists()) {

						} else {
							log4j.error("Could not copy {} to {}, exitting !!", currFile.getAbsolutePath(),	newFile.getAbsolutePath());
							System.exit(-1);
						}
					} catch (IOException e) {
						log4j.error("Could not copy {} to {}, exitting !!", currFile.getAbsolutePath(),
								newFile.getAbsolutePath());
						System.exit(-1);
					}
				}

				// log4j.info("{} New STC File : {} ", rowCount, newFile.getAbsolutePath());
				String id = getId(currFileName);
				int foundRecs = verifyDataForAbookAndAuthor(id);
				if (foundRecs < 1) {
					log4j.info("{} No associated records found !!", rowCount, id, bookTitle, author);
				}

				Cell cellNewFile = rowObject.createCell(1);
				cellNewFile.setCellValue(newFilename);

//				cellProcessed = rowObject.createCell(4);
//				cellFoundBook = rowObject.createCell(5);
//
				cellProcessed = rowObject.createCell(4);
				cellProcessed.setCellValue(id);
//				cellFoundBook.setCellValue(foundBook);
				
				org.bson.Document linkDoc = new org.bson.Document();
				linkDoc.put("key", id);
				linkDoc.put("stcFile", newFile.getName());
				linkDoc.put("bookTitle", bookTitle);
				linkDoc.put("author", author);
				insertOrReplaceGoldLinks(id, linkDoc);
				log4j.info("");
			}
			saveSheet();

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

	public String getId(String currName) {
		Pattern pattern = Pattern.compile("^\\d+");
		Matcher matcher = pattern.matcher(currName);

		String id = null;
		// If a match is found, return the number
		if (matcher.find()) {
			id = matcher.group();
		}
		return id;
	}
	public void insertOrReplaceGoldLinks(String id, Document newdoc) {
		Document filter = new Document("key", id);
		FindIterable<Document> result = goldLinksColl.find(filter);
		Document linkRecord = result.first();
		
		if (linkRecord != null) {
			goldLinksColl.deleteMany(filter); // delete existing first to refresh
		}
		
		goldLinksColl.insertOne(newdoc);
	
	}

	private int verifyDataForAbookAndAuthor(String id) {
//       Document titleRegex = new Document("$regex", bookTitle).append("$options", "i");
//       Document authorRegex = new Document("$regex", author).append("$options", "i");
//       Document filter = new Document("bookTitle", titleRegex).append("author", authorRegex);

		// Document filter = new Document("matchRatio", new Document("$gt", tolerance));
		Document filter = new Document("key", id);

		FindIterable<Document> result = goldBooksColl.find(filter);
		MongoCursor<Document> cursor = result.cursor();
		int totalDocuments = 0;

		while (cursor.hasNext()) {
			Document item = cursor.next();
			totalDocuments++;
		}
		return totalDocuments;
	}

	private String formNewFileName(String currFileName, String _bookName, String _author) {

		String id = getId(currFileName);
		// If a match is found, return the number
		if (id == null) {
			log4j.info("STC File {} does not have an id !! Exitting", currFileName);
			System.exit(-1);
		}
		String ext = GlobalUtils.prepareFileKey(GlobalUtils.getFileExtension(currFileName));
		String bookName = GlobalUtils.prepareFileKey(_bookName);
		String author = GlobalUtils.prepareFileKey(_author);
		String newkey = String.format("%s-%s-%s", id, bookName, author);
		// String newkey = bookName;
		String newFileName = newkey + "." + ext;
		return newFileName;
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
		CompileMediaFilesAndData instance;
		try {
			instance = new CompileMediaFilesAndData(args[0]);
			instance.runSheet();
		} catch (UnknownDataChannelException e) {
			e.printStackTrace();
		}

	}

}
