package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.DelimitedFileLoader;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.FileLedger;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

// Doesn't work through a database but over a file
public class BooktextImporterFile {
	public static final Logger logger = LogManager.getLogger("BooktextImporterFile");

	File sourceDir = null;
	String targetDBConn = null;

	FileIterator txtFileIter = null;
	File currFile = null;

	FileLedger ledger = null;

	// Using a file as database source and use a HashMap to prevent duplicates
	File dbInFile = null;
	Map<String, String> dbContent = new LinkedHashMap<>();

	FileIterator dbFileIter = null;

	ArrayList<String> skippedList = new ArrayList<String>();

	TextFileReader txtInFileReader = null;
	FileExporter dbExporter = null;

	public BooktextImporterFile(File sourceDir, String ledgerFileStr, String dbFileStr) {
		try {
			txtFileIter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ledger = new FileLedger(ledgerFileStr);
			logger.info("Reading ledger file: \"{}\" ...", ledger.getLedgerFile().getAbsolutePath());
			ledger.loadLedger();
			logger.info("Found {} entries in the ledger file.", ledger.getLedgerLength());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		dbInFile = new File(dbFileStr);

		txtInFileReader = new TextFileReader(dbInFile);
		String line = null;
		try {
			while ((line = txtInFileReader.readLine()) != null) {
				String[] rowData = line.split("\\t");
				dbContent.put(rowData[0], rowData[1]);
			}
			logger.info("Read {} records from sourcefile: \"{}\"", dbContent.size(), dbInFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void runFiles() {
		int fileCount = 0;
		File currSourceFile = txtFileIter.getCurrFile();

		try {
			while (currSourceFile != null) {
				logger.info("[{}] Current source file: \"{}\"", fileCount, currSourceFile.getAbsolutePath());
				boolean processed = ledger.isInledger(currSourceFile.getAbsolutePath());
				if (processed) {
					logger.info("[{}] Skipping file: \"{}\" as it was already processed !", fileCount,
							currSourceFile.getAbsolutePath());
					// The file was already processed, jump over
					currSourceFile = txtFileIter.moveToNextTarget();
					fileCount++;
					continue;
				} else {
					logger.info("[{}] File: \"{}\" is not found in the ledger, processing ..", fileCount,
							currSourceFile.getAbsolutePath());
				}

				TextFileReader reader = new TextFileReader(currSourceFile);
				boolean opened = reader.openFile();
				String content = null;
				if (opened) {
					content = reader.readFile();
					HashSet<String> wordsArray = StringProcessor.convertToKeywords(content);
					Iterator<String> iter = wordsArray.iterator();

					int insertCount = 0;
					while (iter.hasNext()) {
						String word = iter.next();
						word = word.replace("-", "");

						// check if the word is already in the database

						boolean existing = dbContent.containsKey(word);
						if (!existing) {
							dbContent.put(word, currSourceFile.getName());
							insertCount += 1;

						} else {
							logger.debug("Skipping word as it already exists ! >>  \"{}\"", word);
						}

					}
					logger.info("[{}] Inserted {} records from \"{}\" .", fileCount, insertCount,
							currSourceFile.getName());
					ledger.addtoLedger(currSourceFile.getAbsolutePath());
				} else {
					logger.info("[{}] Source file {} could not be opened ! Skipping.", fileCount,
							currSourceFile.getAbsolutePath());
					skippedList.add(currSourceFile.getAbsolutePath());
				}

				currSourceFile = txtFileIter.moveToNextTarget();
				fileCount++;
			}
			// write to the database to the new dbfile
			try {
				Map<String, String> sortedMap = new TreeMap<>();

				Iterator<String> iter = dbContent.keySet().iterator();

				while (iter.hasNext()) {
					String word = iter.next();
					String book = dbContent.get(word);
					sortedMap.put(word, book);
				}

				dbFileIter = new FileIterator(dbInFile);
				File nextDBFile = dbFileIter.getNextFile();
				logger.info("Next dbfile : {}, writing \"{}\" records in total.", nextDBFile.getAbsolutePath(),
						dbContent.size());
				dbExporter = new FileExporter(nextDBFile);
				Iterator<String> iterSorted = sortedMap.keySet().iterator();
				while (iterSorted.hasNext()) {
					String word = iterSorted.next();
					String book = dbContent.get(word);
					String line = word + "\t" + book;
					dbExporter.writeLineToFile(line);
				}
				dbExporter.closeExportFile();
				logger.info("New file written, export completed");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exitting");
			ledger.closeLedger();
			System.exit(-1);
		}
		logger.info("BooktestImport process completed ..");
		if (skippedList.size() > 0) {
			ledger.closeLedger();
			logger.warn("Skipped {} files : ", skippedList.size());
			for (int i = 0; i < skippedList.size(); i++) {
				logger.warn("\t[] Skipped file {}  ", i + 1, skippedList.get(i));
			}
		}
	}

	public static void main(String[] args) {
		
		//StringProcessor.writeSpecialCharactersToAFile("C:\\tmp\\test\\SpecialChars.txt", "C:\\tmp\\test\\SpecialChars.lst");
	
		StringProcessor.setSpecialCharsFile("C:\\tmp\\test\\SpecialChars.lst");
		
		StringProcessor.cleanTextFile("C:\\tmp\\test\\SpecialChars.txt");
		
		System.exit(0);
		
		CommandLineArgs argsObject = new CommandLineArgs(args);

		String sourceDirPath = argsObject.get("--inputdir");

		String ledgerFilePath = argsObject.get("--ledgerfile");
		String targetDBFilePath = argsObject.get("--dbinfile");

		File sourceDir = new File(sourceDirPath);

		BooktextImporterFile process = new BooktextImporterFile(sourceDir, ledgerFilePath, targetDBFilePath);
		process.runFiles();

	}
}
