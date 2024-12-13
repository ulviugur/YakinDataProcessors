package com.langpack.process.wordstatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.FileLedger;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

// Doesn't work through a database but over a file
// Imports raw text into a file as they come. 
// !! Does not create a unique list 
public class BooktextRawImporterToFile {
	public static final Logger logger = LogManager.getLogger("BooktextImporterFile");

	File sourceDir = null;
	String targetDBConn = null;

	FileIterator txtFileIter = null;
	File currFile = null;

	FileLedger ledger = null;

	// Using a file as database source and use a HashMap to prevent duplicates
	File dbInFile = null;

	FileIterator dbFileIter = null;

	ArrayList<String> skippedList = new ArrayList<String>();

	TextFileReader txtInFileReader = null;
	FileExporter dbExporter = null;

	public BooktextRawImporterToFile(File sourceDir, String ledgerFileStr, String dbFileStr,
			String specialCharsFilePath) {
		StringProcessor.setSpecialCharsFile(specialCharsFilePath);
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

		try {
			dbFileIter = new FileIterator(dbInFile);
			File nextDBFile = dbFileIter.getNextFile();
			dbExporter = new FileExporter(nextDBFile);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
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
						String word1 = iter.next();
						String word = word1.replaceAll(StringProcessor.SPECIAL_CHARS_REGEX, "");
						String book = currSourceFile.getName();

						String line = word + "\t" + book;
						dbExporter.writeLineToFile(line);

						insertCount += 1;

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

			dbExporter.closeExportFile();
			logger.info("New file written, export completed");
			ledger.closeLedger();

		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exitting");

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

		// StringProcessor.writeSpecialCharactersToAFile("C:\\tmp\\test\\SpecialChars.txt",
		// "C:\\tmp\\test\\SpecialChars.lst");

		// StringProcessor.setSpecialCharsFile("C:\\tmp\\test\\SpecialChars.lst");

		// StringProcessor.cleanTextFile("C:\\tmp\\test\\SpecialChars.txt");

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String sourceDirPath = argsObject.get("--inputdir");

		String ledgerFilePath = argsObject.get("--ledgerfile");
		String targetDBFilePath = argsObject.get("--dbinfile");

		String specialCharsFile = argsObject.get("--specialcharsfile");

		File sourceDir = new File(sourceDirPath);

		BooktextRawImporterToFile process = new BooktextRawImporterToFile(sourceDir, ledgerFilePath, targetDBFilePath,
				specialCharsFile);
		process.runFiles();

	}
}
