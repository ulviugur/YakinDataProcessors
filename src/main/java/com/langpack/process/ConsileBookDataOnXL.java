package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.common.GlobalUtils;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.dbprocess.XLDBInterface_1;
import com.langpack.scraper.PageParser;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class ConsileBookDataOnXL {

	public static final Logger log4j = LogManager.getLogger("ConsileBookDataOnXL");

	XLDBInterface_1 xlInterface = null;

	Boolean importData = false;
	Boolean pageFound = false;
	PageParser parser = null;

	DataChannel sourceData = null;

	String sourceFileStr = null;
	String targetFileStr = null;
	String exceptionFileStr = null;

	File sourceFile = null;
	File targetFile = null;
	File exceptionFile = null;
	
	FileExporter exporterMatched = null;
	FileExporter exporterExceptions = null;

	protected ConfigReader cfg = null;

	private static String TITLE_REGEX = "(\\d{5})_(.+?)_\\((.+)\\)\\.([a-z]+)";

	String mongoURL = "mongodb://localhost:27017";
	// MongoCollection<Document> libLinksColl = null;
	MongoCollection<Document> libBooksColl = null;

	MongoDatabase database = null;
	MongoClient mongoClient = null;

	HashSet<String> existingKeys = new HashSet<String>();

	public ConsileBookDataOnXL(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceFileStr = cfg.getValue("BooksInFile");
		sourceFile = new File(sourceFileStr);

		targetFileStr = cfg.getValue("BooksOutFile");
		targetFile = new File(targetFileStr);
		
		exceptionFileStr = cfg.getValue("BooksExceptionFile");
		exceptionFile = new File(exceptionFileStr);
				
		try {
			exporterMatched = new FileExporter(targetFile);
			exporterExceptions = new FileExporter(exceptionFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		parser = new PageParser();

		sourceData = DataChannelFactory.getChannelByName("XLFileIn");

		mongoClient = MongoClients.create();
		database = mongoClient.getDatabase("test");

		libBooksColl = database.getCollection("librarybooks");
	}

	public void runSourceFile() {
		try {
			XSSFRow rowObject = (XSSFRow) sourceData.getNextRow(); // skip title row
			int lineCount = 2;
			int allCount = 1;
	        exporterMatched.writeLineToFile("UUID\tFilename\tBookName\tAuthor\tFoundBookName\tFoundAuthorName\tKeysCombi");
	        exporterExceptions.writeLineToFile("UUID\tFilename\tBookName\tAuthor\tFoundBookName\tFoundAuthorName\tKeysCombi");
	        
			while (true) {

				rowObject = (XSSFRow) sourceData.getNextRow();
				if (rowObject == null) {
					break;
				}
				Cell cellFilename = rowObject.getCell(0);
				String filename = GlobalUtils.getCellContentAsString(cellFilename);

				Cell cellBookName = rowObject.getCell(2);
				String bookName = GlobalUtils.getCellContentAsString(cellBookName);

				Cell cellAuthor = rowObject.getCell(3);
				String author = GlobalUtils.getCellContentAsString(cellAuthor);

				log4j.info("Line [{}] [ file: {}, name: {}, author: {} ", lineCount, filename, bookName, author);
				
				String keysCombi = bookName + "#" + author;
				
		        Document filter = new Document("keysCombi", keysCombi);
		        FindIterable<Document> records = libBooksColl.find(filter);
		        MongoCursor<Document> cursor = records.iterator();

		        while (cursor.hasNext()) {
		        	Document doc = cursor.next();
		        	String foundBookName = (String)doc.get("bookTitle");
		        	String foundAuthorName = (String)doc.get("author");

		        	ArrayList<String> record = new ArrayList<String>();
		        	UUID uuid = UUID.randomUUID();
		        	record.add(uuid.toString());
		        	record.add(filename);
		        	record.add(bookName);
		        	
		        	String correctedAuthor = correctAuthor(author, foundAuthorName); // returns foundAuthor if its tokens are the same 
		        	record.add(correctedAuthor);
		        	record.add(foundBookName);
		        	record.add(foundAuthorName);
		        	record.add(keysCombi);
		        	String recordStr = GlobalUtils.convertArraytoString(record, "\t");
		        	log4j.info("{}#{} ->  {}", lineCount, allCount, recordStr);
		        	
		        	boolean canExport = checkValidForExport(bookName, foundBookName, correctedAuthor, foundAuthorName);
		        	if (canExport) {
		        		exporterMatched.writeLineToFile(recordStr);
		        	} else {
		        		exporterExceptions.writeLineToFile(recordStr);
		        	}
		        	allCount++;
		        }
		        

				lineCount++;
			}
			exporterMatched.closeExportFile();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info("Process completed ..");
	}
	

	private boolean checkValidForExport(String bookName, String foundBookName, String oriAuthor, String foundAuthor) {
		
	    // Split the book and author names into arrays of words
	    String[] partsOriBookName = bookName.split(" ");
	    String[] partsOriAuthor = oriAuthor.split(" ");
	    String[] partsFoundBookName = foundBookName.split(" ");
	    String[] partsFoundAuthor = foundAuthor.split(" ");
	    
	    // Convert arrays to sets for easier comparison
	    Set<String> setOriBookName = new HashSet<>(Arrays.asList(partsOriBookName));
	    Set<String> setOriAuthor = new HashSet<>(Arrays.asList(partsOriAuthor));
	    Set<String> setFoundBookName = new HashSet<>(Arrays.asList(partsFoundBookName));
	    Set<String> setFoundAuthor = new HashSet<>(Arrays.asList(partsFoundAuthor));

	    // Check if either book name or author has enough overlap to consider it valid
	    boolean isBookNameMatching = hasWordOverlap(setOriBookName, setFoundBookName);
	    boolean isAuthorMatching = hasWordOverlap(setOriAuthor, setFoundAuthor);

	    // Return true if either book name or author matches sufficiently
	    boolean retval = isBookNameMatching && isAuthorMatching;
	    return retval;
	}

	private boolean hasWordOverlap(Set<String> original, Set<String> found) {
	    // Check if there's any overlap between the two sets (tolerance for partial matches)
	    for (String word : original) {
	        if (found.contains(word)) {
	            return true;  // Found at least one word that matches
	        }
	    }
	    return false;  // No matching words found
	}
	
	private String correctAuthor(String oriAuthor, String foundAuthor) {
		
		if (oriAuthor.equals("Adıvar Halide Edib")) {
			log4j.info(oriAuthor);
		}
		String[] partsOri = oriAuthor.split(" ");
		String[] partsFound = foundAuthor.split(" ");
		
        Arrays.sort(partsOri);
        Arrays.sort(partsFound);
        
        if (Arrays.equals(partsOri, partsFound)) {
        	return foundAuthor;
        } else {
        	return oriAuthor;
        }
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ConsileBookDataOnXL instance;
		try {
			instance = new ConsileBookDataOnXL(args[0]);
			instance.runSourceFile();
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
