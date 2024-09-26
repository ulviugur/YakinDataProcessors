package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileExporter;
import com.langpack.common.GlobalUtils;

import zemberek.core.io.Files;

public class FilterBooksAndGenerateIndexes {
	public static final Logger logger = LogManager.getLogger("FilterBooksAndGenerateIndexes");

	File booksDir = null;
	File listsDir = null;

	File uniquesFile = null;
	File duplicatesFile = null;
	File fullListFile = null;
	File uqDir = null;

	String fileBase = null;

	String idRegexString = "\\d{5}";
	Pattern pattern = Pattern.compile(idRegexString);

	// Lists to store duplicates and unique keys
	TreeSet<String> duplicateKeys = new TreeSet<String>();
	TreeSet<String> uniqueKeys = new TreeSet<String>();
	ArrayList<String> indexKeys = new ArrayList<String>();

	TreeMap<String, TreeSet<String>> nameMap = new TreeMap<String, TreeSet<String>>();

	public FilterBooksAndGenerateIndexes(String dbFileStr, String listdirStr, String uqDirStr) {
		booksDir = new File(dbFileStr);
		listsDir = new File(listdirStr);
		uqDir = new File(uqDirStr);

		File[] filesList = booksDir.listFiles();

		for (int i = 0; i < filesList.length; i++) {
			File tmpFile = filesList[i];
			String _name = tmpFile.getName();
			String baseName = GlobalUtils.getFileBase(_name);
			TreeSet<String> keyWords = getKeywordsList(baseName);

			nameMap.put(_name, keyWords);
			// String tmpString = GlobalUtils.convertArraytoString(keyWords, " # ");
			// logger.info(tmpString);
		}
		uniquesFile = new File(listsDir, "uniques.txt");
		duplicatesFile = new File(listsDir, "duplicates.txt");
		fullListFile = new File(listsDir, "full.txt");
	}

	private TreeSet<String> getKeywordsList(String name) {
		Matcher matcher = pattern.matcher(name);
		String plainName = name;
		if (matcher.find()) {
			String found = matcher.group();
			// logger.info("MATCH : {}", name);
			plainName = plainName.replace(found, "");
			// logger.debug("");
			// there is a XXXXX identifier in the name
			// remove the id from the name for comparison
		} else {
			// no id file
			// logger.info("NO_MATCH : {}", name);
		}
		plainName = plainName.replace("'", "");
		plainName = plainName.replace("’", "");
		plainName = plainName.replace(",", "");
		plainName = plainName.replace("-", "");

		// logger.info("Plain name : {}", plainName);
		TreeSet<String> retval = new TreeSet<String>();
		String[] tmpArray = plainName.split("\\_|\\(|\\)");

		for (int i = 0; i < tmpArray.length; i++) {
			String item = tmpArray[i];
			if (!("".equals(item))) {
				retval.add(item);
			}
		}

		return retval;
	}

	public void findDuplicatesAndUniques(TreeMap<String, TreeSet<String>> nameMap) {

		// HashMap to track first occurrence of TreeSet values
		Map<TreeSet<String>, String> seenMap = new HashMap<>();

		// Iterate through the TreeMap in a single loop
		for (Map.Entry<String, TreeSet<String>> entry : nameMap.entrySet()) {
			String key = entry.getKey();
			TreeSet<String> valueSet = entry.getValue();

			// Check if the same TreeSet content has been seen before
			if (seenMap.containsKey(valueSet)) {
				duplicateKeys.add(key); // Add to duplicates
			} else {
				uniqueKeys.add(key); // Add to unique keys
				seenMap.put(valueSet, key); // Mark this TreeSet as seen
			}
		}
	}

	public void writeList(File listFile, TreeSet<String> keysObject) {
		try {
			FileExporter ex = new FileExporter(listFile);
			Iterator<String> iter = keysObject.iterator();
			while (iter.hasNext()) {
				String item = iter.next();
				ex.writeLineToFile(item);
			}
			ex.closeExportFile();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void writeList(File listFile, ArrayList<String> keysObject) {
		try {
			FileExporter ex = new FileExporter(listFile);
			Iterator<String> iter = keysObject.iterator();
			while (iter.hasNext()) {
				String item = iter.next();
				ex.writeLineToFile(item);
			}
			ex.closeExportFile();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void createIndexKeys() {
		Iterator<String> iter = nameMap.keySet().iterator();
		int count = 0;
		while(iter.hasNext()) {
			String key = iter.next();
			TreeSet<String> item = nameMap.get(key);
			String keywordsStr = GlobalUtils.convertArraytoString(item);
			String line = keywordsStr + "=>" + key;
			indexKeys.add(line);
			logger.info("[{}] : {}", count, line);
			count++;
		}
	}
	public void copyUQFiles() {
		Iterator<String> iter = uniqueKeys.iterator();
		while (iter.hasNext()) {
			String item = iter.next();
			File oldItemFile = new File (booksDir, item);
			File newItemFile = new File (uqDir, item);
			logger.info("{} => {}", oldItemFile.getAbsolutePath(), newItemFile.getAbsolutePath());
			try {
				Files.copy(oldItemFile, newItemFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void runProcess() {
		findDuplicatesAndUniques(nameMap);

		createIndexKeys();
		
		writeList(uniquesFile, uniqueKeys);
		writeList(duplicatesFile, duplicateKeys);
		writeList(fullListFile, indexKeys);
		copyUQFiles();
		logger.info("Process completed");
		logger.info("");
	}

	public static void main(String[] args) {
		CommandLineArgs argsObject = new CommandLineArgs(args);
		String booksDirPath = argsObject.get("--booksdir");
		String listDirPath = argsObject.get("--listdir");
		String uqDirPath = argsObject.get("--uqdir");
		FilterBooksAndGenerateIndexes process = new FilterBooksAndGenerateIndexes(booksDirPath, listDirPath, uqDirPath);
		process.runProcess();
	}
}
