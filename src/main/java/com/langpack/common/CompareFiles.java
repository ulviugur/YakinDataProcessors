package com.langpack.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompareFiles {

	public static final Logger log4j = LogManager.getLogger("CompareFiles");

	File file1 = null;
	File file2 = null;

	File reportFile = null;
	FileExporter exporter = null;

	String splitChar = ",";
	int[] indexFields = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
	TreeMap<String, String[]> dataArray1 = new TreeMap<>();
	TreeMap<String, String[]> dataArray2 = new TreeMap<>();

	ConfigReader cfg = null;

	private TreeMap<String, String[]> loadDataset(File inFile) {
		log4j.info("Starting to load file : " + inFile.getAbsolutePath());
		TreeMap<String, String[]> tmpDataArray = new TreeMap<>();
		TextFileReader reader1 = new TextFileReader(inFile);
		String line1 = null;
		if (reader1.openFile()) {
			try {
				while ((line1 = reader1.readLine()) != null) {
					String[] fields = line1.split(splitChar);
					StringBuffer key = new StringBuffer();
					for (int indexField : indexFields) {
						String token = fields[indexField];
						key.append(token);
						key.append("-");
					}
					String finalKey = key.substring(0, key.length() - 1);
					tmpDataArray.put(finalKey, fields);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			log4j.info(String.format("Error in opening File-1 : %s", file1.getAbsolutePath()));
			System.exit(-1);
		}
		return tmpDataArray;
	}

	public CompareFiles(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);
		String reportFileStr = cfg.getValue("DifferenceReportFile");
		reportFile = new File(reportFileStr);

		try {
			exporter = new FileExporter(reportFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Running constructor ..");
		String str1 = cfg.getValue("File1");
		file1 = new File(str1);
		dataArray1 = loadDataset(file1);

		String str2 = cfg.getValue("File2");
		file2 = new File(str2);
		dataArray2 = loadDataset(file2);

	}

	public void process() {
		int diffCount = 0;
		log4j.info("Starting the process ..");
		for (String key1 : dataArray1.keySet()) {
			log4j.debug("Checking line " + key1);

			String[] values1 = dataArray1.get(key1);
			String[] values2 = dataArray2.get(key1);

			if (values1.length != values2.length) {
				log4j.info(String.format("Inconsistent data array lengths between 2 datasets !! <%s vs. %s> ",
						Integer.toString(values1.length), Integer.toString(values2.length)));
			} else {
				String str1 = GlobalUtils.convertArrayToString(values1);
				str1 = str1.replaceAll(" ", "");
				String str2 = GlobalUtils.convertArrayToString(values2);
				str2 = str2.replaceAll(" ", "");
				if (!str1.equals(str2)) {
					exporter.writeLineToFile("1:" + str1);
					exporter.writeLineToFile("2:" + str2);
					diffCount++;
				}
			}
		}

		log4j.info(String.format("Found %s differences between files", diffCount));

	}

	public static void main(String[] args) {
		log4j.info("Started processing CompareData..");
		CompareFiles instance = new CompareFiles(args[0]);

		instance.process();

		log4j.info("Finished processing CompareData");
	}

}
