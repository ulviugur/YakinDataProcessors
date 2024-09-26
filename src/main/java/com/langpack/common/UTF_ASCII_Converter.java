package com.langpack.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Normalizer;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UTF_ASCII_Converter {
	public static final Logger log4j = LogManager.getLogger("UTF_ASCII_Converter");

	public static void main(String[] args) {

		// TODO Auto-generated method stub
		File inputFile = new File(args[0]);

		TextFileReader reader = new TextFileReader(inputFile, "UTF-8");

		FileExporter exporter = null;
		try {
			exporter = new FileExporter(args[0] + ".new", "UTF-8");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		log4j.info("Reading file : " + inputFile.getAbsolutePath());
		log4j.info("Will write file : " + exporter.getExportFile().getAbsolutePath());
		String line = null;
		int count = 0;
		try {
			while ((line = reader.readLine()) != null) {

				String newLine = convert(line);
				exporter.writeLineToFile(newLine);
				count++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info(String.format("Exported %s lines, closing files ..", count));
		exporter.closeExportFile();
		reader.closeFile();
		log4j.info("Finished ..");

	}

	public static String convert(String tmp) {
		String nfdNormalizedString1 = Normalizer.normalize(tmp, Normalizer.Form.NFD);
		Pattern pattern1 = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		String test1 = pattern1.matcher(nfdNormalizedString1).replaceAll("").toUpperCase();
		return test1;
	}

}
