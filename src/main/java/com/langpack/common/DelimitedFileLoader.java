package com.langpack.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DelimitedFileLoader {

	private HashMap<Integer, ArrayList<String>> dataset = new HashMap<>();

	public DelimitedFileLoader(String string) {
		// TODO Auto-generated constructor stub
	}

	public HashMap<Integer, ArrayList<String>> getDataset() {
		return dataset;
	}

	private final Charset cset = Charset.forName("UTF-8");

	public static final Logger log4j = LogManager.getLogger("TextFileLoader");

	public void loadFile(String sourceFileName, int skipLines) {
		File importFile = new File(sourceFileName);
		loadFile(importFile, skipLines);
	}

	public void loadFile(File importFile, int skipLines) {
		log4j.info(String.format("Loading file '%s' started ..", importFile.getAbsolutePath()));

		int fileReadCount = 0;

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(importFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStreamReader freader = new InputStreamReader(fstream, cset);
		BufferedReader reader = new BufferedReader(freader, 1024);

		String line = null;
		int maxLength = 0;
		HashSet<String> localities = new HashSet<>();
		ArrayList<String> fields = null;
		try {
			while ((line = reader.readLine()) != null) {
				fileReadCount++;
				if (fileReadCount > skipLines) {
					// log4j.info("Line :" + line);
					line.replace("\u0000", "");
					fields = parseLine(line);
					// log4j.info("Line length : " + fields.size());
					localities.add(fields.get(0));
					if (fields.get(0).length() > maxLength) {
						maxLength = fields.get(0).length();
					}
					dataset.put(Integer.valueOf(fileReadCount), fields);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Read " + fileReadCount + " lines from " + importFile.getAbsolutePath());
	}

	private ArrayList<String> parseLine(String tmpLine) {
		ArrayList<String> retval = new ArrayList<>();
		String[] columns = tmpLine.split(",|;");
		for (String column : columns) {
			retval.add(column);
		}
		return retval;
	}

	private ArrayList<String> splitLine(String tmpLine) {

		ArrayList<String> retval = new ArrayList<>();
		boolean openQuotes = false;
		boolean skipNext = false; // in case quotes are closed, skip the next comma
		String field = "";
		for (int i = 0; i < tmpLine.length(); i++) {
			String letter = tmpLine.substring(i, i + 1);
			if (letter.equals("\"")) {
				if (openQuotes) {
					retval.add(field);
					field = "";
					openQuotes = false;
					skipNext = true;
				} else {
					openQuotes = true;
				}
			} else if (letter.equals(",") && !openQuotes) {
				if (!skipNext) {
					retval.add(field);
					field = "";
				} else {
					skipNext = false;
				}
			} else {
				field = field + letter;
			}
		}
		return retval;
	}

	public static void main(String[] args) {
		log4j.info("Starting loading file ..");

		// TODO Auto-generated method stub
		DelimitedFileLoader instance = new DelimitedFileLoader(args[0]);
		instance.loadFile("S:/Ulvi/workspace/GeoSystems/data/Locality/JuneDelivery/Final/ALB.txt", 1);

		log4j.info("Finishing process");
	}

}
