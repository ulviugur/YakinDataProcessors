package com.langpack.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.dbprocess.LangsLoader;	

public class StringProcessor {
	public static Locale turkishLocale = new Locale.Builder().setLanguage("tr").setRegion("TR").build();
	private static TextFileReader specialCharsFileReader = null;
	public static final Logger logger = LogManager.getLogger("BooktextImporterFile");
	private static HashSet<String> SPECIAL_CHARS_ARRAY = new HashSet<String>();

	public static String SPECIAL_CHARS_REGEX = null;

	public static String cleanBookString(String input) {

		// Replace each period with a period followed by a single new line
		String step1 = input.replaceAll("\\.", ".\n");

		// Remove multiple new lines (replace sequences of new lines with a single new
		// line)
		String step2 = step1.replaceAll("\\n+", "\n");

		// Trim leading spaces from each line and manage spaces at the beginning of
		// lines
		// Using a regex to match lines with leading spaces and trim them
		String step3 = step2.replaceAll("(?m)^\\s+", "");

		// Optionally, remove trailing spaces at the end of each line
		String step4 = step3.replaceAll("(?m)\\s+$", "");

		// Remove itemized lists
		String step5 = step4.replaceAll("\\n.\\n|\\r\\n\\.\\r\\n", "");

		// Remove multiple spaces
		String step6 = step5.replaceAll("\\s+", " ");

		// remove all non.printable characters
		String regex = "[\\x00-\\x1F\\x7F]";
		String step7 = step6.replaceAll(regex, " ");

		String step8 = step7.replaceAll("\\u00AD", "");

		return step8;
	}

	public static HashSet<String> convertToKeywords(String input) {

		String filterRegex = "\\s+|\\.|['‘’′“”«»–—]|\\d+|,|!|\"|;|\\?|:";
		String[] rawWords = input.split(filterRegex);

		HashSet<String> filteredWords = new HashSet<String>();

		// drop invaluable data
		for (int i = 0; i < rawWords.length; i++) {
			String tmpWord = rawWords[i];
			if (tmpWord.matches(filterRegex) || "".equals(tmpWord.trim()) || tmpWord.length() < 3) {
				// skipping token
			} else {
				filteredWords.add(tmpWord.toLowerCase(turkishLocale));
			}

		}
		return filteredWords;
	}

	public static void writeSpecialCharactersToAFile(String sampleFilePathStr, String specialCharsFileStr) {
		TextFileReader rdr = new TextFileReader(sampleFilePathStr);
		String content = rdr.readFile();
		String[] lines = content.split("\r\n");

		Pattern pattern = Pattern.compile("([^a-zA-ZçÇğĞıİöÖşŞüÜ0-9()\\r\\n])");
		HashSet<String> specs = new HashSet<String>();

		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);

			while (matcher.find()) {
				String part = matcher.group(1);
				specs.add(part);
			}
		}
		FileExporter export = null;
		try {
			export = new FileExporter(specialCharsFileStr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String item : specs) {
			export.writeLineToFile(item);
		}

		export.closeExportFile();
	}

	public static void setSpecialCharsFile(String tmpFileStr) {
		specialCharsFileReader = new TextFileReader(tmpFileStr);
		String content = specialCharsFileReader.readFile();
		String[] lines = content.split("\r\n");

		StringBuffer sb = new StringBuffer();
		// sb.append("[^a-zA-ZçÇğĞıİöÖşŞüÜ");
		sb.append("[");
		int count = 1;
		for (String line : lines) {
			String newChar = "\\" + line;
			SPECIAL_CHARS_ARRAY.add(newChar);
			sb.append(line);

			// test character in regex
			logger.info("[{}] Testing {} :", count, newChar);
			String testRegex = sb.toString().concat("]");
			"test".replaceAll(testRegex, "");
			logger.info("Tested :" + newChar);
			count++;
		}
		logger.info("Added {} lines of special characters to be stripped from text.", lines.length);
		sb.append("]");

		SPECIAL_CHARS_REGEX = sb.toString();

		logger.info("Regex for special characters : {}", SPECIAL_CHARS_REGEX);

	}

	// When books are translated into text strings, process them to clean
	public static String removeSpecialCharacters(String input) {
		if (specialCharsFileReader == null) {
			logger.error("Special characters file is not set; use StringProcessor.setSpecialCharsFile() method");
			logger.error("Exitting !!");
			System.exit(-1);
		}

		String filterExp1 = "[()] | # | \\$ | = | & | % | \\* | \\+ | \\- |\\[ | \\]|";
		String retval = input.replaceAll(filterExp1, "");
		return retval;

	}

	public static void cleanTextFile(String inFileStr) {
		// load English words from database to remove any of them from book text
		TreeSet<String> langs = LangsLoader.loadLangs("localhost", 27017, "test", "langs", "EN");

		TextFileReader reader = new TextFileReader(inFileStr);

		FileIterator fiter = null;
		File outFile = null;
		FileExporter exporter = null;
		try {
			fiter = new FileIterator(new File(inFileStr));
			outFile = fiter.getNextFile();
			exporter = new FileExporter(outFile);

		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		String line = null;
		int lineCount = 1;
		TreeSet<String> words = new TreeSet<String>();
		try {
			while ((line = reader.readLine()) != null) {
				String newLine = line.replaceAll(StringProcessor.SPECIAL_CHARS_REGEX, "");
				if ("".equals(newLine)) {
					logger.info("Skipping line {} as it lead to empty string ..", lineCount);
				} else {
					if (!langs.contains(newLine)) {
						words.add(newLine);
					} else {
						logger.info("Skipping line {} as it is an English word {} ..", lineCount, newLine);
					}
				}
				lineCount++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String word : words) {
			exporter.writeLineToFile(word);
		}

		exporter.closeExportFile();

	}

}
