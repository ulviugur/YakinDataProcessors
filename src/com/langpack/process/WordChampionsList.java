package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.GlobalUtils;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.TextFileReader;
import com.yakin.morphtr.AnalysisWrapper;
import com.yakin.morphtr.WordModel;
import com.yakin.morphtr.WordType;

// Doesn't work through a database but over a file
// Imports raw text into a file as they come. 
// !! Does not create a unique list 
public class WordChampionsList {
	public static final Logger logger = LogManager.getLogger("WordChampionsList");

	File sourceDir = null;
	String targetDBConn = null;

	File currFile = null;

	// Using a file as database source and use a HashMap to prevent duplicates
	File dbInFile = null;
	HashMap<String, Integer> dbContent = new HashMap<String, Integer>();

	FileIterator dbFileIter = null;

	TextFileReader txtInFileReader = null;
	FileExporter dbExporter = null;

	AnalysisWrapper analysis = new AnalysisWrapper();

	String fileBase = null;
	
	Set<Integer> exportArray = new TreeSet<Integer>();

	public WordChampionsList(String dbFileStr) {

		dbInFile = new File(dbFileStr);
		
		String ext = GlobalUtils.getFileExtension(dbInFile);
		String baseRaw = GlobalUtils.getFileBase(dbInFile);
		String base = baseRaw.replaceAll("_v1\\..*", "");
		String champsFileName = base + "_CHAMPS." + ext;
		File dbFileDir = dbInFile.getParentFile(); 
		File champsFile = new File (dbFileDir, champsFileName);

		try {
			dbFileIter = new FileIterator(champsFile);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Collections.addAll(exportArray, 100, 200, 500, 1000, 1500, 2000, 5000, 10000, 20000, 50000, 100000, 1000000);
	}

	private void tickWord(String _word) {
		Integer count = dbContent.get(_word);
		if (count == null) {
			count = 1;
		} else {
			count++;
		}
		dbContent.put(_word, count);
		if (exportArray.contains(count)) {
			exportState();
			exportArray.remove(count);
		}
		
	}
	
	private void exportState() {
		File exportFile = dbFileIter.getNextFile();
		List<Entry<String, Integer>> sortedList = sortElements();

		try {
			FileExporter expStems = new FileExporter(exportFile);
			for (Map.Entry<String, Integer> entry : sortedList) {
				expStems.writeLineToFile(entry.getKey() + ": " + entry.getValue());
			}
			expStems.closeExportFile();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<Entry<String, Integer>> sortElements() {

		// Step 2: Convert the HashMap entries to a list and sort it
		List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(dbContent.entrySet());
		sortedEntries.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
		return sortedEntries;
	}

	public void runProcess() {
		TextFileReader reader = new TextFileReader(dbInFile);
		boolean opened = reader.openFile();
		if (opened) {

			int insertCount = 0;
			int lineCount = 0;
			String line = null;

			AnalysisWrapper aw = new AnalysisWrapper();

			try {
				while ((line = reader.readLine()) != null) {
					String[] words = line.split("\t");
					String word = words[0];
					if ("".equals(word.trim())) {
						continue;
					}
					lineCount++;

					if (lineCount % 10 == 0) {
						logger.info("[{}] Word : {}", lineCount, word);
					}

					try {
						List<WordModel> results = aw.getWordAnalysis(word);
						for (int i = 0; i < results.size(); i++) {
							WordModel res = results.get(i);
							WordType wt = res.getType();

							if (wt == WordType.EXACT_MATCH) {
								String root = res.getRootWord();
								tickWord(root);
							}

						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} // end of while
				exportState();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // end of try

			/*
			 * List<Entry<String, Integer>> sortedList = sortElements();
			 * 
			 * String fileStemsStr = fileBase + "_STEMS.txt"; File fileStems = new
			 * File(currSourceFile.getParent(), fileStemsStr);
			 * 
			 * try { FileExporter expStems = new FileExporter(fileStems); for
			 * (Map.Entry<String, Integer> entry : sortedList) {
			 * expStems.writeLineToFile(entry.getKey() + ": " + entry.getValue()); }
			 * expStems.closeExportFile();
			 * 
			 * } catch (FileNotFoundException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}

		logger.info("Process completed");
		logger.info("");

	}

	public static void main(String[] args) {

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String targetDBFilePath = argsObject.get("--dbinfile");

		WordChampionsList process = new WordChampionsList(targetDBFilePath);

		process.runProcess();

	}
}
