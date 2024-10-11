package com.langpack.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
import com.mongodb.client.result.InsertManyResult;

public class MergeBooksData {

	public static final Logger log4j = LogManager.getLogger("BinBooksScarepeIntoMongo");

	XLDBInterface_1 xlInterface = null;

	String sourceFileStr = null;
	File sourceFile = null;

	DataChannel sourceData = null;

	protected ConfigReader cfg = null;

	// ArrayList<String> bookKeys = new ArrayList<String>();

	String mongoURL = "mongodb://localhost:27017";
	MongoCollection<Document> BinColl = null;
	MongoCollection<Document> DNRColl = null;

	MongoCollection<Document> goldBooksColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	XLChannel xlObject = null;
	XSSFWorkbook wb = null;

	public static String[] BENCHMARK_FIELDS = new String[] { "bookTitle", "author", "publisher" };
	public static String[] PRINT_FIELDS = new String[] { "_id", "bookTitle", "author", "publisher" };
	private static String MATCH_RATIO_FIELDNAME = "matchRatio";

	public MergeBooksData(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksFile");
		sourceFile = new File(sourceFileStr);

		sourceData = DataChannelFactory.getChannelByName("XLFile");
		xlObject = (XLChannel) sourceData;

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");
		BinColl = database.getCollection("BinBooks");
		DNRColl = database.getCollection("DNRBooks");
		goldBooksColl = database.getCollection("GoldBooks");

		wb = xlObject.getWorkbookObject();

		boolean retval = saveSheet();
		if (!retval) {
			log4j.info("File {} cannot be opened to process, close the file if it is open. Exitting !!");

			System.exit(-1);
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

	public void runSheet() {
		int runonly = 30000; // only run a subset after skipping the amount and if not (marked as) processed
								// yet
		int skipRows = 1;
		XSSFRow rowObject = null;
		for (int i = 0; i < skipRows; i++) {
			try {
				rowObject = (XSSFRow) sourceData.getNextRow();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // skip title row
		}

		try {
			while (true) {
				rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}

				Cell cellCurrFile = rowObject.getCell(0);
				String currFile = GlobalUtils.getCellContentAsString(cellCurrFile);

				Cell cellNewFile = rowObject.getCell(1);
				String newFile = GlobalUtils.getCellContentAsString(cellNewFile);

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

				String id = getId(currFile);

				ArrayList<Document> goldCopy = processLine(id, bookName, author);

				if (!goldCopy.isEmpty()) {
					InsertManyResult result = null;

					List<Integer> insertedIds = new ArrayList<>();
					// Inserts sample documents and prints their "_id" values

					InsertManyResult success = goldBooksColl.insertMany(goldCopy);
					
					//success.getInsertedIds().values().forEach(doc -> insertedIds.add(doc.toString()));
					//System.out.println("Inserted documents with the following ids: " + insertedIds);

					log4j.info("Inserted {} documents for {} / {}..", goldCopy.size(), bookName, author);
					Document example = goldCopy.get(0);
					Double matchRatio = (Double) example.get(MATCH_RATIO_FIELDNAME);
					if (matchRatio.equals(1.00)) {
						// book title from sheet is already valid
						foundBook = "Y";
					} else {
						// book title is potentially different
						Cell cellUpdateTitle = rowObject.createCell(6);
						cellUpdateTitle.setCellValue(example.getString("bookTitle"));

						Cell cellUpdateAuthor = rowObject.createCell(7);
						cellUpdateAuthor.setCellValue(example.getString("author"));
						foundBook = "M";
					}
				}

				cellProcessed = rowObject.createCell(4);
				cellFoundBook = rowObject.createCell(5);

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

	// returns exact and tolerated answers
	public ArrayList<Document> searchInCollection(String collName, String id, String _book, String _author) {
		ArrayList<Document> retval = new ArrayList<Document>();
		MongoCollection<Document> coll = database.getCollection(collName);
		FindIterable<Document> foundRecords = coll.find();
		MongoCursor<Document> cursor = foundRecords.iterator();

		int count = 0;
		while (cursor.hasNext()) {
			Document record = cursor.next();
			record.put("key", id);
			String bookTitle = (String) record.get("bookTitle");
			String author = (String) record.get("author");

			if (author.equals(_author) && bookTitle.equals(_book)) {
				log4j.info("[{}] Exact Match : {} x {} = {} x {}", count, _book, _author, bookTitle, author);
				record.put("matchRatio", 1.00);
				retval.add(record);
			} else {
				String foundTitle = null;
				try {
					foundTitle = compareItems(_book, bookTitle);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				if (foundTitle != null) { // Book Passed
					String priAuthor = author;
					String altAuthor = extractAlternativeName(author);
					String foundAuthor = null;
					if (altAuthor != null) {
						foundAuthor = compareItems(_author, altAuthor);
						if (foundAuthor == null) { // alternative name was not relevant
							String tmpAuthor = author.replace(altAuthor, "").replace("()", "").trim();
							foundAuthor = compareItems(_author, tmpAuthor);
							if (foundAuthor != null) {
								double dist = LevensteinIndex.relativeIndex(_book + _author, bookTitle + tmpAuthor);
								record.put("matchRatio", dist);
								log4j.info("[{}] Found : {} x {} = {} x {}", count, _book, _author, bookTitle,
										tmpAuthor);
								retval.add(record);
							}
						}
					} else {
						foundAuthor = compareItems(_author, author);
						if (foundAuthor != null) {
							double dist = LevensteinIndex.relativeIndex(_book + _author, bookTitle + author);
							record.put("matchRatio", dist);
							log4j.info("[{}] Found : {} x {} = {} x {}", count, _book, _author, bookTitle, author);
							retval.add(record);
						}
					}

				}
			}
			count++;
		}
		return retval;
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

	public static String extractAlternativeName(String fullAuthor) {

		int startIndex = fullAuthor.indexOf("(");
		int endIndex = fullAuthor.indexOf(")");
		if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
			return fullAuthor.substring(startIndex + 1, endIndex).trim();
		}
		return null;
	}

	private String compareItems(String searchItem, String knownItem) {

		String retval = null;

		String cleanSearchItem = searchItem.replace(".", " ").replace("-", " ").replace("  ", " ");
		String cleanKnownItem = knownItem.replace(".", " ").replace("-", " ").replace("  ", " ");

		// BOF - CASE1
		String normalSearchItem = GlobalUtils.normalizeUnicodeStringToUpper(cleanSearchItem);
		String normalKnownItem = GlobalUtils.normalizeUnicodeStringToUpper(cleanKnownItem);

		String[] partsSearchItem = normalSearchItem.split(" ");
		String[] partsKnownItem = normalKnownItem.split(" ");

		Set<String> setSearchItem = new TreeSet<>(Arrays.asList(partsSearchItem));
		Set<String> setKnownItem = new TreeSet<>(Arrays.asList(partsKnownItem));

		// int matchCount = matchingKeys(setSearchItem, setKnownItem);

		int dist = LevensteinIndex.distance(GlobalUtils.convertArraytoString(partsSearchItem),
				GlobalUtils.convertArraytoString(partsKnownItem));
		if (dist <= setSearchItem.size()) {
			retval = knownItem;
			return retval;
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

	// remove all repeating records
	public ArrayList<Document> flattenList(ArrayList<Document> tmpList) {
		ArrayList<Document> retval = new ArrayList<Document>();
		if (tmpList.size() == 0) {
			return retval;
		} else {
			Document tmp = tmpList.get(0);
			tmp.remove("_id");
			retval.add(tmp); // add the first element, check the rest
		}
		for (int i = 1; i < tmpList.size(); i++) {
			Document newItem = tmpList.get(i);
			boolean shouldDrop = false;
			for (int j = 0; j < retval.size(); j++) {
				Document existingItem = retval.get(j);
				log4j.info(GlobalUtils.getDocumentFields(newItem, PRINT_FIELDS) + " == "
						+ GlobalUtils.getDocumentFields(existingItem, PRINT_FIELDS));
				boolean tmp = docsEqual(existingItem, newItem);
				if (tmp) {
					shouldDrop = true;
					break;
				}
			}
			if (!shouldDrop) {
				newItem.remove("_id");
				retval.add(newItem);
			}
		}
		return retval;
	}

	public ArrayList<Document> processLine(String id, String book, String author) {
		ArrayList<Document> fullList2 = searchInCollection("DNRBooks", id, book, author);
		ArrayList<Document> fullList1 = searchInCollection("BinBooks", id, book, author);

		ArrayList<Document> fullList = new ArrayList<Document>();
		fullList.addAll(fullList1);
		fullList.addAll(fullList2);
		log4j.info("BinBooks : {}, DNRBooks : {}, Total : {}", fullList1.size(), fullList2.size(), fullList.size());
		double maxMatch = 0.0;
		for (Document doc : fullList) {
			Double mr = (Double) doc.get("matchRatio");
			if (mr >= maxMatch) {
				maxMatch = mr;
			}
		}
		ArrayList<Document> bestList = new ArrayList<Document>();
		for (Document doc : fullList) {
			Double mr = (Double) doc.get("matchRatio");
			if (mr == maxMatch) {
				doc.put("localTNFilename", makeLocalTNFilename(doc));
				bestList.add(doc);
			}
		}

		ArrayList<Document> flatList = flattenList(bestList);
		log4j.info("FlattenedListSize : {}", flatList.size());
		return flatList;
	}
	public static String makeLocalTNFilename(Document doc) {
		String key = (String) doc.get("key");
		String thumbnailURL = (String) doc.get("thumbnailURL");
		String ext = GlobalUtils.prepareFileKey(GlobalUtils.getFileExtension(thumbnailURL));
		String bookName = GlobalUtils.prepareFileKey((String) doc.get("bookTitle"));
		String author = GlobalUtils.prepareFileKey((String) doc.get("author"));
		String publisher = GlobalUtils.prepareFileKey((String) doc.get("publisher"));
		String index = GlobalUtils.prepareFileKey(Integer.toString((Integer) doc.get("index")));
		String newkey = null;
		try {
			newkey = String.format("%s-%s-%s-%s-%s", key, bookName, author, publisher, index);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// String newkey = bookName;
		String localFileName = newkey + "." + ext;
		return localFileName;
	}

	public static void main(String[] args) {
		// System.out.println("classpath=" + System.getProperty("java.class.path"));

		MergeBooksData instance;
		try {
			instance = new MergeBooksData(args[0]);
			// ArrayList<Document> tmp = instance.searchInCollection("BinBooks", "Uzay
			// 1999", "Edwin Charles Tubb");
			log4j.info("");
			instance.runSheet();

		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
