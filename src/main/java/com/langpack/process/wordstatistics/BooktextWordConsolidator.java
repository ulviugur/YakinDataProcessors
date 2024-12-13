package com.langpack.process.wordstatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

// Doesn't work through a database but over a file
// Imports raw text into a file as they come. 
// !! Does not create a unique list 
public class BooktextWordConsolidator {
	public static final Logger logger = LogManager.getLogger("BooktextWordConsolidator");

	File sourceDir = null;
	String targetDBConn = null;

	File currFile = null;

	// Using a file as database source and use a HashMap to prevent duplicates
	File dbInFile = null;
	TreeSet<String> dbContent = new TreeSet<>();

	FileIterator dbFileIter = null;

	ArrayList<String> skippedList = new ArrayList<String>();

	TextFileReader txtInFileReader = null;
	FileExporter dbExporter = null;

	public BooktextWordConsolidator(String dbFileStr) {

		dbInFile = new File(dbFileStr);

		try {
			dbFileIter = new FileIterator(dbInFile);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void runProcess() {
		File currSourceFile = dbFileIter.getCurrFile();

		TextFileReader reader = new TextFileReader(currSourceFile);
		boolean opened = reader.openFile();
		if (opened) {

			int insertCount = 0;
			int lineCount = 0;
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					String[] lineArray = line.split("\t");
					String word = lineArray[0];
					lineCount++;
					
					boolean existing = dbContent.contains(word);
					if (!existing) {
						dbContent.add(word);
						insertCount += 1;
						logger.info("Inserted {}/{} records from dictionary \"{}\" .", insertCount, lineCount,
								currSourceFile.getName());
					} else {
						logger.debug("Skipping word as it already exists ! >>  \"{}\"", word);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			logger.info("[{}] Source file {} could not be opened ! Skipping.", currSourceFile.getAbsolutePath());
			skippedList.add(currSourceFile.getAbsolutePath());
		}

		// write to the database to the new dbfile
		try {

			File nextDBFile = dbFileIter.getNextFile();
			logger.info("Next dbfile : {}, writing \"{}\" records in total.", nextDBFile.getAbsolutePath(),
					dbContent.size());
			dbExporter = new FileExporter(nextDBFile);
			Iterator<String> iterDict = dbContent.iterator();
			while (iterDict.hasNext()) {
				String word = iterDict.next();
				dbExporter.writeLineToFile(word);
			}
			dbExporter.closeExportFile();
			logger.info("New file  {} written, export completed", nextDBFile.getAbsolutePath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String targetDBFilePath = argsObject.get("--dbinfile");

		BooktextWordConsolidator process = new BooktextWordConsolidator(targetDBFilePath);

		process.runProcess();

	}
}
