package com.langpack.process.golddata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langpack.common.ConfigReader;
import com.langpack.common.EPUBContentReader;
import com.langpack.common.FileExporter;
import com.langpack.common.GlobalUtils;
import com.langpack.common.LevensteinIndex;
import com.langpack.scraper.BinBooksScraper;
import com.langpack.scraper.BookScraper;
import com.langpack.scraper.DNRBooksScraper;
import com.langpack.scraper.DownloadThumbnails;
import com.langpack.scraper.PageParser;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class BookDataConsolidator {

	public static final Logger log4j = LogManager.getLogger("BookDataConsolidator");
	
	public static String[] BENCHMARK_FIELDS = new String[] { "correctedBookname", "correctedAuthor", "newEPUBFilename", "STCFilename" };

	Boolean pageFound = false;
	PageParser parser = null;
	String sourceFileStr = null;

	File rawEPUBDir = null;
	File newEPUBDir = null;
	File newSTCDir = null;

	File TNDestFolder = null;

	File sourceFile = null;

	protected ConfigReader cfg = null;

	// Integer maxid = 0;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String mongoURL = "mongodb://localhost:27017";
	MongoCollection<Document> libLinksColl = null;
	MongoCollection<Document> libBooksColl = null;

	MongoCollection<Document> goldLinksColl = null;
	MongoCollection<Document> goldBooksColl = null;
	MongoCollection<Document> oldBooksColl = null;
	MongoCollection<Document> bookIndexColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XSSFWorkbook wb = null;
	XSSFSheet pendingsSheet = null;
	XSSFSheet indexSheet = null;

	FileInputStream fsIP = null;

	Integer oriFilenameColumnNumber;
	Integer cleanFilenameColumnNumber;
	Integer keyColumnNumber;
	Integer oriBookNameColumnNumber;
	Integer oriAuthorColumnNumber;

	Integer bookNameColumnNumber;
	Integer authorColumnNumber;
	Integer processedColumnNumber;
	Integer foundBookColumnNumber;
	Integer EPUBFilenameColumnNumber;
	Integer STCFilenameColumnNumber;
	Integer skipLines;
	Integer PROCESS_MAX;

	Boolean copyNewEPUBFiles = false;
	Boolean overwriteSTCFiles = false;
	Boolean scrapeData = false;

	XSSFCellStyle styleChanged = null;
	XSSFCellStyle styleUnknown = null;

	BinBooksScraper binBooksScraper = new BinBooksScraper();
	DNRBooksScraper dNRBooksScraper = new DNRBooksScraper();

	// will use to load the mapping of the gold files / Ids
	// TreeMap<String, Integer> goldMap = new TreeMap<String, Integer>();

	// Supported the multiple stages of consolidating data
	// 1- Using the old "Gold" reference sheet for creating a wider list.
	// (loadGoldSheetMap())
	// 2- Assigning missing Ids to the orphan (no id) records (addIdFields())
	// 3- Therefore finding the maximum Id (getMaxId())
	// 4- Scraping all data from DNR and 1000Kitap and capturing the data in the
	// database (runSheet()). This has to be running after each update on the work
	// sheet to organise and capture data

	public BookDataConsolidator(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);
		String pendingsSheetname = cfg.getValue("PendingsSheet");
		String indexSheetName = cfg.getValue("IndexSheet");

		try {
			fsIP = new FileInputStream(sourceFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Access the workbook
		log4j.info(String.format("Loading file %s ..", sourceFile.getAbsoluteFile()));
		try {
			wb = new XSSFWorkbook(fsIP);
			pendingsSheet = wb.getSheet(pendingsSheetname);
			indexSheet = wb.getSheet(indexSheetName);

			// if (newWorkSheet != null) {
			// log4j.warn("workSheet : {} already exists, Exitting in order to prevent
			// overwriting data !!",
			// newWorkSheetName);
			// System.exit(-1);
			// }
			//
			// if (newGoldSheet != null) {
			// log4j.warn("New GoldSheet : {} already exists, Exitting in order to prevent
			// overwriting data !!",
			// newGoldSheetName);
			// System.exit(-1);
			// }
			//
			// newWorkSheet = wb.createSheet(newWorkSheetName);
			// newGoldSheet = wb.createSheet(newGoldSheetName);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		parser = new PageParser();

		mongoClient = MongoClients.create();

		String dbName = cfg.getValue("mongo.dbName");
		database = mongoClient.getDatabase(dbName);

		String rawLinksCollName = cfg.getValue("mongo.rawLinksCollName", "liblinks");
		libLinksColl = database.getCollection(rawLinksCollName);

		String rawBooksCollName = cfg.getValue("mongo.rawBooksCollName", "libbooks");
		libBooksColl = database.getCollection(rawBooksCollName);

		String goldLinksCollName = cfg.getValue("mongo.GoldLinksCollName", "GoldLinks");
		goldLinksColl = database.getCollection(goldLinksCollName);

		String goldBooksCollName = cfg.getValue("mongo.GoldBooksCollName", "GoldBooks");
		goldBooksColl = database.getCollection(goldBooksCollName);

		String bookIndexCollName = cfg.getValue("mongo.BookIndexCollName", "BookIndex");
		bookIndexColl = database.getCollection(bookIndexCollName);

		oldBooksColl = database.getCollection("OldBooks");

		rawEPUBDir = new File(cfg.getValue("RawEpubDirectory"));
		newEPUBDir = new File(cfg.getValue("NewEpubDirectory"));
		newSTCDir = new File(cfg.getValue("NewSTCDirectory"));

		oriFilenameColumnNumber = Integer.parseInt(cfg.getValue("Field.OriFilenameColumnNumber", "0"));

		cleanFilenameColumnNumber = Integer.parseInt(cfg.getValue("Field.CleanFilenameColumnNumber", "1"));

		keyColumnNumber = Integer.parseInt(cfg.getValue("Field.IdColumnNumber", "2"));

		oriBookNameColumnNumber = Integer.parseInt(cfg.getValue("Field.OriBookNameColumnNumber", "3"));
		oriAuthorColumnNumber = Integer.parseInt(cfg.getValue("Field.OriAuthorColumnNumber", "4"));

		bookNameColumnNumber = Integer.parseInt(cfg.getValue("Field.BookNameColumnNumber", "5"));
		authorColumnNumber = Integer.parseInt(cfg.getValue("Field.AuthorColumnNumber", "6"));
		processedColumnNumber = Integer.parseInt(cfg.getValue("Field.ProcessedColumnNumber", "7"));
		foundBookColumnNumber = Integer.parseInt(cfg.getValue("Field.FoundBookColumnNumber", "8"));
		EPUBFilenameColumnNumber = Integer.parseInt(cfg.getValue("Field.EPUBFilenameColumnNumber", "9"));
		STCFilenameColumnNumber = Integer.parseInt(cfg.getValue("Field.STCFilenameColumnNumber", "10"));
		skipLines = Integer.parseInt(cfg.getValue("SkipLines", "1"));
		PROCESS_MAX = Integer.parseInt(cfg.getValue("ProcessMax", "1"));

		copyNewEPUBFiles = Boolean.valueOf(cfg.getValue("Action.CopyNewEPUBFiles", "false"));
		overwriteSTCFiles = Boolean.valueOf(cfg.getValue("Action.OverwriteSTCFiles", "false"));
		scrapeData = Boolean.valueOf(cfg.getValue("Action.ScrapeData", "false"));

		String TNFolderName = cfg.getValue("TNFolder");
		TNDestFolder = new File(TNFolderName);

		boolean retval = saveSheet();
		if (!retval) {
			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");
			System.exit(-1);
		}

		styleChanged = wb.createCellStyle();
		styleChanged.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		styleChanged.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		styleUnknown = wb.createCellStyle();
		styleUnknown.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		styleUnknown.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// after the id allocations, no need to run this
		// loadGoldSheetMap();
		// getMaxId();
		// log4j.info("Maxid : " + maxid);
	}

	// 1- Skip if the record is already marked as processed
	// 2- Ignore records marked as "ignore"
	// 3- Check if book data is already collected (checkExistingScrapedData())
	// 4- If scraped book data is not found, run both scrapers
	// 5- Find the best fitting book name and capture it if it is within tolerance
	// (<2 distance). This might lead to false positives
	public void runPendings() {
		String methodName = new Throwable().getStackTrace()[0].getMethodName();
		log4j.info("Started {}()..", methodName);
		int rowCount = skipLines;
		// int nextId = 90168;
		int processCount = 0;
		try {
			XSSFRow rowObject = null;
			while ((rowObject = (XSSFRow) pendingsSheet.getRow(rowCount)) != null) {

				boolean confirmedBooknameWasReady = false;

				Cell celloriFilename = rowObject.getCell(oriFilenameColumnNumber);
				String oriFilename = GlobalUtils.getCellContentAsString(celloriFilename); // EPUB original filename

				Cell cellBookName = rowObject.getCell(bookNameColumnNumber);
				String fullBookName = GlobalUtils.getCellContentAsString(cellBookName);
				if (fullBookName == null || "".equals(fullBookName)) {
					Cell cellOriBookName = rowObject.getCell(oriBookNameColumnNumber);
					fullBookName = GlobalUtils.getCellContentAsString(cellOriBookName);
				} else {
					confirmedBooknameWasReady = true;
				}

				Cell cellAuthor = rowObject.getCell(authorColumnNumber);
				String fullAuthor = GlobalUtils.getCellContentAsString(cellAuthor);

				Cell cellProcessed = rowObject.getCell(processedColumnNumber);
				String processed = GlobalUtils.getCellContentAsString(cellProcessed);

				Cell cellKey = rowObject.getCell(keyColumnNumber);
				String key = GlobalUtils.getCellContentAsString(cellKey);

				if ("Y".equals(processed)) {
					log4j.info("[{}] Skipping {}#{} as it was already processed .. ", rowCount, fullBookName,
							fullAuthor);
					rowCount++;
					continue;
				} else if (fullAuthor.contains("ignore")) {
					log4j.info("[{}] Skipping {}#{} as it should be ignored .. ", rowCount, fullBookName, fullAuthor);
					rowCount++;
					continue;
				} else {
					HashSet<String> bookNamesList = new HashSet<String>();
					if (scrapeData) {
						ArrayList<Document> scrapedData = checkScrapedLibBooks(fullBookName, fullAuthor);
						
						if (scrapedData != null && scrapedData.size() > 0) { // if already available, no need to collect further
							log4j.info("Skipping to scrape {}#{} as they already exist .. ", fullBookName, fullAuthor);
							for (Iterator<Document> iterator = scrapedData.iterator(); iterator.hasNext();) {
								Document tmpDoc = (Document) iterator.next();
								String name = (String) tmpDoc.get("bookName");
								bookNamesList.add(name);
							}
						} else {
							log4j.info("Scraping book data {}#{} .. ", fullBookName, fullAuthor);

							HashSet<String> bookList1 = runScraper(binBooksScraper, key, fullBookName, fullAuthor);
							HashSet<String> bookList2 = runScraper(dNRBooksScraper, key, fullBookName, fullAuthor);

							bookNamesList.addAll(bookList1);
							bookNamesList.addAll(bookList2);
						}

						String bestBookName = null;

						int diffToName = 100;
						Iterator<String> iter = bookNamesList.iterator();
						while (iter.hasNext()) {
							String itemName = iter.next();
							int dist = LevensteinIndex.distance(itemName, fullBookName);
							if (dist < diffToName) {
								bestBookName = itemName;
								diffToName = dist;
							}
						}
						if (!confirmedBooknameWasReady) {
							cellBookName = rowObject.createCell(bookNameColumnNumber);
							cellBookName.setCellValue(bestBookName);
							cellBookName.setCellStyle(styleChanged); // Originally was not confirmed, updated now
						}

						cellProcessed = rowObject.createCell(processedColumnNumber);
						cellProcessed.setCellValue("Y");
						
						// ***************  Decision : Pending sheet can be only processed to find the best BookName
						// ***************  Only a checker human can confirm the record as found ("FoundBook"="Y") if the scraped details are acceptable
						// Cell cellFoundBook = rowObject.createCell(foundBookColumnNumber);
//						if (diffToName < 2) { // if the differences between names is minor, it is treated as a hit
//							cellFoundBook.setCellValue("Y");
//						} else {
//							cellFoundBook.setCellValue("N"); // until we find a better method, distance 2 is a "N"
//						}

						saveSheet();

						Thread.sleep(1000);

					} else {
						log4j.info("Action.ScrapeData is set to false, skipping this step ..");
					}
				}

				rowCount++;
				processCount++;

				if (processCount >= PROCESS_MAX) {
					break;
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		saveSheet();
		log4j.info("Completed process runSheet() !");
	}

	public HashSet<String> runScraper(BookScraper scraper, String id, String fullBookName, String fullAuthor) {
		HashSet<String> bookNameArray = new HashSet<String>();

		Object linksObject = null;
		ArrayList<String> links = null;

		Document linkDoc = scraper.collectBookLinks(fullBookName, fullAuthor);
		if (linkDoc != null) {

			libLinksColl.insertOne(linkDoc);
			linksObject = linkDoc.get("links");

			if (linksObject instanceof ArrayList<?>) {
				links = (ArrayList<String>) linksObject;
			} else {
				System.out.println("Unexpected object type, exitting !!");
				System.exit(-1);
			}

			for (int index = 0; index < links.size(); index++) {
				String scrapeURL = links.get(index);

				Document bookData = scraper.scrapeBookData(scrapeURL, fullBookName, fullAuthor, index);
				if (bookData != null) {
					String tmpBookName = (String) bookData.get("bookName");
					bookNameArray.add(tmpBookName);
					bookData.put("key", id);
					libBooksColl.insertOne(bookData);
				}
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return bookNameArray;
	}

	public void insertOrReplaceGoldLinks(String key, Document newdoc) {
		Document filter = new Document("key", key);
		FindIterable<Document> result = goldLinksColl.find(filter);
		Document linkRecord = result.first();

		if (linkRecord != null) {
			goldLinksColl.deleteMany(filter); // delete existing first to refresh
		}

		goldLinksColl.insertOne(newdoc);

	}

	// run though the whole sheet and get the maximum id of the sheet so that we can
	// allocate further ids
	// only run once during id allocations
	/*
	 * private Integer getMaxId() { int rowCount = skipLines;
	 * 
	 * XSSFRow rowObject = null; while ((rowObject = (XSSFRow)
	 * workSheet.getRow(rowCount)) != null) { XSSFCell cellId =
	 * rowObject.getCell(idColumnNumber); String idStr =
	 * GlobalUtils.getCellContentAsString(cellId); // log4j.info(" idStr : " +
	 * idStr); if (idStr == null || "".equals(idStr)) { // empty value } else {
	 * Integer id = Integer.parseInt(idStr); if (id > maxid) { maxid = id; } } //
	 * log4j.info("Processing row : {}", rowCount); rowCount++; } return maxid; }
	 */

	/*
	 * private void processEPUBFiles() {
	 * 
	 * int rowCount = skipLines;
	 * 
	 * XSSFRow rowObject = null; while ((rowObject = (XSSFRow)
	 * workSheet.getRow(rowCount)) != null) { // log4j.info("Processing row : {}",
	 * rowCount);
	 * 
	 * XSSFCell cellOrifilename = rowObject.getCell(0); String oriFileName =
	 * GlobalUtils.getCellContentAsString(cellOrifilename);
	 * 
	 * XSSFCell cellId = rowObject.getCell(idColumnNumber); String id =
	 * GlobalUtils.getCellContentAsString(cellId);
	 * 
	 * XSSFCell cellBookname = rowObject.getCell(bookNameColumnNumber); String
	 * bookName = GlobalUtils.getCellContentAsString(cellBookname);
	 * 
	 * XSSFCell cellAuthor = rowObject.getCell(authorColumnNumber); String author =
	 * GlobalUtils.getCellContentAsString(cellAuthor);
	 * 
	 * String foundBook = checkIfBookIsFound(bookName, author);
	 * 
	 * Cell cellFoundBook = rowObject.createCell(foundBookColumnNumber);
	 * cellFoundBook.setCellValue(foundBook);
	 * 
	 * if (bookName == null || author == null || "".equals(bookName) ||
	 * author.contains("ignore") || !"Y".equals(foundBook)) { rowCount++; continue;
	 * } else if (id != null) {
	 * 
	 * File realFile = findMatchingFile(oriFileName); log4j.info("[{}] : {} -> {}",
	 * rowCount, oriFileName, realFile.getName());
	 * 
	 * String newFileName = formNewFileName(id.toString(), bookName, author,
	 * "epub");
	 * 
	 * File srcFile = realFile; File dstFile = new File(newEPUBDir, newFileName);
	 * 
	 * Path srcPath = Paths.get(srcFile.getAbsolutePath()); Path dstPath =
	 * Paths.get(dstFile.getAbsolutePath());
	 * 
	 * try { // Move the file to the target directory with a new name //
	 * Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
	 * log4j.info("{} => {}", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
	 * } catch (Exception e) {
	 * System.err.println("Error occurred while moving the file: " +
	 * e.getMessage()); e.printStackTrace(); }
	 * 
	 * }
	 * 
	 * rowCount++; } saveSheet(); log4j.info("Process completed .."); }
	 */

	/*
	 * private String checkIfBookIsFound(String verifiedBookName, String
	 * verifiedAuthor) { String retval = "N"; Document filter1 = new
	 * Document("bookName", verifiedBookName); filter1.put("author",
	 * verifiedAuthor);
	 * 
	 * FindIterable<Document> result = libBooksColl.find(filter1);
	 * 
	 * if (result.first() != null) { retval = "Y"; } return retval; }
	 */

	private File findMatchingFile(String oriFileName) {
		String oriId = getKey(oriFileName);
		File matchedFile = null;
		File[] files = rawEPUBDir.listFiles();
		int distance = 100;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String name = file.getName();

			String currId = getKey(name);
			if (currId != null && currId.equals(oriId)) {
				return file;
			} else {
				int dist = LevensteinIndex.distance(oriFileName, name);
				if (dist < distance) {
					matchedFile = file;
					distance = dist;
				}
			}
		}

		return matchedFile;
	}

	// Process row if a record is marked as processed and updated
	//   - Create EPUB and STC files with the validated names
	//   - Update the cells with the filenames (NewFilename (J column) and STCName (K column)
	//   - Update the index records in Mongo.BookIndex collection
	public void updateIndexRecords() {
		log4j.info("Started updateIndexRecords()");
		int rowNumber = 0;

		rowNumber++;

		XSSFRow rowObject = null;
		while ((rowObject = (XSSFRow) indexSheet.getRow(rowNumber)) != null) {

			XSSFCell cellOrifilename = rowObject.getCell(0);
			String oriFileName = GlobalUtils.getCellContentAsString(cellOrifilename);

			XSSFCell cellCleanFilenameColumnNumber = rowObject.getCell(cleanFilenameColumnNumber);
			String cleanFileName = GlobalUtils.getCellContentAsString(cellCleanFilenameColumnNumber);

			XSSFCell cellKey = rowObject.getCell(keyColumnNumber);
			String key = GlobalUtils.getCellContentAsString(cellKey);

			XSSFCell cellOriBookname = rowObject.getCell(oriBookNameColumnNumber);
			String oriBookname = GlobalUtils.getCellContentAsString(cellOriBookname);

			XSSFCell cellOriAuthor = rowObject.getCell(oriAuthorColumnNumber);
			String oriAuthor = GlobalUtils.getCellContentAsString(cellOriAuthor);

			XSSFCell cellBookname = rowObject.getCell(bookNameColumnNumber);
			String bookName = GlobalUtils.getCellContentAsString(cellBookname);

			XSSFCell cellAuthor = rowObject.getCell(authorColumnNumber);
			String author = GlobalUtils.getCellContentAsString(cellAuthor);

			XSSFCell cellProcessed = rowObject.getCell(processedColumnNumber);
			String processed = GlobalUtils.getCellContentAsString(cellProcessed);

			XSSFCell cellFound = rowObject.getCell(foundBookColumnNumber);
			String found = GlobalUtils.getCellContentAsString(cellFound);

			File srcFile = findMatchingFile(oriFileName);

			File dstFile = null;
			File stcFile = null;

			String newEPUBFileName = null;
			String newSTCFileName = null;

			log4j.info("[{}] : {} -> {}", rowNumber, key, bookName);

			if ("Y".equals(processed) && "Y".equals(found)) {

				newEPUBFileName = formNewFileName(key.toString(), bookName, author, "epub");
				newSTCFileName = formNewFileName(key.toString(), bookName, author, "stc");

				try {
					dstFile = new File(newEPUBDir, newEPUBFileName);
					stcFile = new File(newSTCDir, newSTCFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}

				Path srcPath = Paths.get(srcFile.getAbsolutePath());
				Path dstPath = Paths.get(dstFile.getAbsolutePath());

				try { // Move the file to the target directory with a new name
					if (!dstFile.exists() && copyNewEPUBFiles) { // if the file already exists, dont copy again
						Path newFilePath = Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
					}
					// log4j.info("Copy successful !! {} => {}", srcFile.getAbsolutePath(),
					// dstFile.getAbsolutePath());
					// if result is not null, it worked successfully !!

					createSTCFile(dstFile, stcFile, overwriteSTCFiles);
				} catch (Exception e) {
					System.err.println("Error occurred while copying the file: " + e.getMessage());
					e.printStackTrace();
				}

			}

			XSSFCell cellEPUBFile = null;
			XSSFCell cellSTCFile = null;

			if (stcFile.exists()) { // only if all of the happy paths could be followed
				XSSFRow newRow = indexSheet.getRow(rowNumber);
				cellEPUBFile = newRow.createCell(EPUBFilenameColumnNumber);
				cellSTCFile = newRow.createCell(STCFilenameColumnNumber);
				cellEPUBFile.setCellValue(newEPUBFileName);
				cellSTCFile.setCellValue(stcFile.getName());

				Document doc = new Document().append("EPUB_OriFilename", oriFileName)
						.append("EPUB_CleanFilename", cleanFileName).append("key", key)
						.append("oriBookname", oriBookname).append("oriAuthor", oriAuthor)
						.append("correctedBookname", bookName).append("correctedAuthor", author)
						.append("newEPUBFilename", newEPUBFileName).append("STCFilename", stcFile.getName())
						.append("updateTime", GlobalUtils.getCurrentTimeInString());

				Document updateInst = new Document("$set", doc);

				Document filter = new Document();
				filter.put("key", key);
				Document existingDoc = bookIndexColl.find(filter).first();
				if (existingDoc != null) {
					boolean result = docsEqual(existingDoc, doc);
					if (result != true) { // significant change
						bookIndexColl.updateOne(filter, updateInst, new UpdateOptions().upsert(true));						
					}
				} else {
					bookIndexColl.insertOne(doc);
				}
			}

			rowNumber++;
			if (rowNumber > PROCESS_MAX) {
				break;
			}
		}
		saveSheet();
		log4j.info("Completed process updateIndexRecords()");
	}

	public void createSTCFile(File currSourceFile, File exportFile, boolean overwrite) {

		String content = null;
		EPUBContentReader instance = new EPUBContentReader();
		try {
			content = instance.extractTextFromEpub(currSourceFile);
			// content = StringProcessor.extractSTCFromText(content1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		// System.out.print(retval);

		if (content == null || content.length() < 100) {
			log4j.warn("[{}] Skipping file: {} as it is empty !", currSourceFile.getAbsolutePath());
			return;
		} else {
			if (exportFile.exists() && !overwrite) {
				log4j.trace("STC File {} already exists, no overwrite instructions. Skipping !",
						exportFile.getAbsoluteFile());
			} else {
				FileExporter fExporter = null;
				try {
					fExporter = new FileExporter(exportFile);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fExporter.writeStringToFile(content);
				fExporter.closeExportFile();
				log4j.trace("Content of {} bytes are written to file {}", content.length(),
						exportFile.getAbsoluteFile());
			}

		}
	}

	// make a filename using the parameters
	public static String formNewFileName(String key, String _bookName, String _author, String _ext) {

		String bookName = GlobalUtils.prepareFileKey(_bookName);
		String author = GlobalUtils.prepareFileKey(_author);

		if (key == null || bookName == null || author == null) {
			return null;
		}
		String newkey = String.format("[%s]-[%s]-[%s]", key, bookName, author);
		// String newkey = bookName;
		String newFileName = newkey + "." + _ext;
		return newFileName;
	}

	// Retrieve key (id) from a String. Since all books have an Id now, it will be
	// needed when we add new books to the library
	public static String getKey(String currName) {
		Pattern pattern = Pattern.compile("^\\d+");
		Matcher matcher = pattern.matcher(currName);

		String id = null;
		// If a match is found, return the number
		if (matcher.find()) {
			id = matcher.group();
		}
		return id;
	}

	private void insertGoldBooks() {
		log4j.info("Started insertGoldBooks()");
		FindIterable<Document> iter = bookIndexColl.find();
		MongoCursor<Document> cursor = iter.cursor();

		int processCount = 0;
		while (cursor.hasNext()) {
			Document indexRecord = cursor.next();

			String key = indexRecord.getString("key");
			String bookName = indexRecord.getString("correctedBookname");
			String author = indexRecord.getString("correctedAuthor");
			String STCFilename = indexRecord.getString("STCFilename");

			log4j.info("[{}] Processing {} => {}, {}", processCount, key, bookName, author);

			// running through all scraped libBook collections with the right bookName / author
			ArrayList<Document> existingLibBooks = checkScrapedLibBooks(bookName, author);
			if (existingLibBooks.size() > 0) {
				//log4j.info("Found already scraped {} records", existingLibBooks.size());
				// Insert all book data which is not already in GoldBooks
				for (int i = 0; i < existingLibBooks.size(); i++) {
					Document newBookRecord = existingLibBooks.get(i); //Publisher book
					String scrapeURL = newBookRecord.getString("scrapeURL");
					String newPublishYear = GlobalUtils.getYearFromString(newBookRecord.getString("publishYear"));
					String newPublisher = newBookRecord.getString("publisher");
					if (newPublisher == null) newPublisher = "";

					// Resetting keysCombi in case the original scrape is linked with a different book. Ex : "Aşk ve Gurur" also brings "Gurur ve Önyargı" books.
					String keysCombi = String.format("%s#%s", bookName, author); 
					newBookRecord.put("keysCombi", keysCombi);
					newBookRecord.put("STCFilename", STCFilename);
					newBookRecord.put("key", key);

					Document filter1 = new Document().append("key", key).append("keysCombi", keysCombi).append("bookName", bookName).append("author", author).append("publisher", newPublisher);
					// find any previously imported records for the same publisher, may be at a later year
					FindIterable<Document> existingRecords = goldBooksColl.find(filter1); 
					Document sample = existingRecords.first(); // Publisher book, if it is already inserted
					if (sample == null) { // if no record is found, just insert
						//log4j.info("Inserting : " + newPublishYear + "_" + newPublisher);
						Document result = DownloadThumbnails.processThumbnail(newBookRecord, TNDestFolder);
						goldBooksColl.insertOne(result);
					} else {
						// if there is already a record, consider replacing it if its PublishYear is later
						MongoCursor<Document> cursor2 = existingRecords.cursor();
						while (cursor2.hasNext()) {
							Document existingRecord = cursor2.next();
							String existingPublishYear = GlobalUtils.getYearFromString(existingRecord.getString("publishYear"));
							String existingPublisher = existingRecord.getString("publisher");
							if (newPublisher.equals(existingPublisher) && Integer.parseInt(newPublishYear) > Integer.parseInt(existingPublishYear)) {
								log4j.info("Replacing in GoldBooks : " + existingPublishYear + "_" + existingPublisher);
								goldBooksColl.replaceOne(filter1, newBookRecord);
							}
						}
						//log4j.info("Compared with all existing records");
					}
				}
			} else {
				log4j.info("****** No existing scraped books found for : [{}] {} : {}#{}", processCount, key, bookName, author);
				///log4j.fatal("Exitting");
				//System.exit(-1);
			}

			processCount++;

			if (processCount >= PROCESS_MAX) {
				break;
			}
		}
		log4j.info("Completed process insertGoldBooks()");
	}

	private ArrayList<Document> checkScrapedLibBooks(String bookName, String author) {
		ArrayList<Document> retval = new ArrayList<Document>();
		HashSet<String> controlSet = new HashSet<String>(); // this set will prevent multiple entries of the same book
		Document filter = new Document();
		filter.put("author", author); // Filter out all book records for exact book / author
		filter.put("bookName", bookName);
		// Using a sorted cursor to get 1000Kitap as a preferred source
		FindIterable<Document> records = libBooksColl.find(filter).sort(new Document("scrapeURL", 1));
		MongoCursor<Document> iter = records.iterator();
		int count = 0;
		// run through all books have-ing the same name and author
		while (iter.hasNext()) {
			Document doc = iter.next();

			// String key = (String) doc.get("key");
			String scrapeURL = (String) doc.get("scrapeURL");
			//log4j.info("[{}], ScrapeURL={}", count, scrapeURL);

			// check in the old GoldBooks to spare internet scraping
			FindIterable<Document> oldRecords = oldBooksColl.find(new Document("scrapeURL", scrapeURL)); 
			Document oldDoc = oldRecords.first();

			String tmpPublisher = null;
			String publisher = null;
			String tmpPublishYear = null;
			String publishYear = null;

			if (oldDoc != null) {
				tmpPublisher = oldDoc.getString("publisher");
				tmpPublishYear = oldDoc.getString("publishYear");
				oldDoc.remove("_id");
			}

			String metadataString = (String) doc.get("metaData");
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, String> metadata = null;
			try {
				metadata = objectMapper.readValue(metadataString, Map.class);
				tmpPublisher = metadata.get("Publisher");

				if (tmpPublisher == null) {
					tmpPublisher = metadata.get("publisher");
				}
				if (tmpPublisher == null) {
					tmpPublisher = metadata.get("Yayınevi");
				}

				if (tmpPublisher == null) {
					tmpPublisher = "###";
					// System.exit(count);
				} else {
					publisher = tmpPublisher;
				}

				tmpPublishYear = metadata.get("Publication Date");

				if (tmpPublishYear == null) {
					tmpPublishYear = metadata.get("Basım Tarihi");
				}

				if (tmpPublishYear == null) {
					tmpPublishYear = "0";
				} else {
					publishYear = tmpPublishYear;
				}

			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if ("###".equals(publisher)) {
				// not good enough, skip it
			} else { // relevant record is found
				String publisherKey = GlobalUtils.prepareFileKey(tmpPublisher);

				String newkey = null;
				try {
					newkey = String.format("%s-%s-%s-%s", bookName, author, publisherKey, publishYear);
					if (newkey.contains("null")) {
						continue;// skip if any parameters is empty
					}
					// log4j.info("{} >>>> {} ", count, newkey);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (controlSet.contains(newkey)) {
					// if already in retval array, dont include this record
				} else {
					doc.remove("_id");
					doc.put("publisher", publisher);
					doc.put("publishYear", publishYear);
					retval.add(doc);
					controlSet.add(newkey);
				}
			}

			count++;
		}
		return retval;
	}

	public boolean checkExistingScrapedDataOverKeysCombi(String keysCombi) {
		Document filter = new Document();
		filter.put("keysCombi", keysCombi);

		FindIterable<Document> records = libBooksColl.find(filter);
		MongoCursor<Document> iter = records.iterator();
		int countDocs = 0;
		while (iter.hasNext()) {
			Document record = iter.next();
			// log4j.info(record);
			countDocs++;
		}
		if (countDocs == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean docsEqual(Document primary, Document candidate) {
		boolean retval = true;
		for (int i = 0; i < BENCHMARK_FIELDS.length; i++) {
			String field = BENCHMARK_FIELDS[i];
			Object priValue = primary.get(field);
			Object newValue = candidate.get(field);

			if (priValue == null && newValue == null) {
				continue;
			} else if (priValue == null && newValue != null) {
				retval = false;
				break;
			} else if (priValue != null && newValue == null) {
				retval = false;
				break;
			} else if (!priValue.equals(newValue)) {
				retval = false;
				break;
			} else if (priValue.equals(newValue)) {
				continue;
			}
		}
		return retval;
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
		BookDataConsolidator instance = null;
		instance = new BookDataConsolidator(args[0]);
		instance.runPendings();
		//instance.updateIndexRecords();
		//instance.insertGoldBooks();
	}

}
