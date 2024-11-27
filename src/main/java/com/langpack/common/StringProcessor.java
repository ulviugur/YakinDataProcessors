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
	public static final Logger logger = LogManager.getLogger("StringProcessor");
	
	public static Locale turkishLocale = new Locale.Builder().setLanguage("tr").setRegion("TR").build();
	private static TextFileReader specialCharsFileReader = null;

	private static HashSet<String> SPECIAL_CHARS_ARRAY = new HashSet<String>();

	public static String SPECIAL_CHARS_REGEX = null;

	// Replaced with the below extractSTCFromText() method
//	public static String cleanBookString(String input) {
//		
//		//logger.info("Input : {}", input);
//
//		// Replace each period with a period followed by a single new line
//		String step1 = input.replaceAll("\\.", ".\n");
//		
//		//logger.info("step1 : {}", step1);
//
//		// Remove multiple new lines (replace sequences of new lines with a single new
//		// line)
//		String step2 = step1.replaceAll("\\n+", "\n");
//		
//		//logger.info("step2 : {}", step2);
//
//		// Trim leading spaces from each line and manage spaces at the beginning of
//		// lines
//		// Using a regex to match lines with leading spaces and trim them
//		String step3 = step2.replaceAll("(?m)^\\s+", "");
//		//logger.info("step3 : {}", step3);
//
//		// Optionally, remove trailing spaces at the end of each line
//		String step4 = step3.replaceAll("(?m)\\s+$", "");
//		//logger.info("step4 : {}", step4);
//
//		// Remove itemized lists
//		String step5 = step4.replaceAll("\\n\\.\\n|\\r\\n\\.\\r\\n", "");
//		//logger.info("step5 : {}", step5);
//
//		// Remove multiple spaces
//		//logger.info("step6 : {}", step6);
//
//		// remove all non.printable characters
//		String regex = "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]";
//		String step7 = step5.replaceAll(regex, " ");
//		//logger.info("step7 : {}", step7);
//
//		String step8 = step7.replaceAll("\\u00AD", "");
//		//logger.info("step8 : {}", step8);
//		
//		String step9 = step8.replaceAll("\\s*,", ", ");
//		//logger.info("step9 : {}", step9);
//		
//		String step10 = step9.replaceAll("“\\s+", "“");
//		//logger.info("step10 : {}", step10);
//		
//		String step11 = step10.replaceAll("\\s+”", "”");
//		//logger.info("step11 : {}", step11);
//		
//		String step12 = step11.replaceAll("(?<=[a-zçğıöşü])(?=[A-ZÇĞİÖŞÜ])", " ").trim();
//		
//		String step13 = step12.replaceAll("“", " “");
//		String step14 = step13.replaceAll("”", "”\n");
//		
//		String step15 = step14.replaceAll("  ", " ");
//				
//		return step15;
//	}

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
			//logger.info("[{}] Testing {} :", count, newChar);
			String testRegex = sb.toString().concat("]");
			"test".replaceAll(testRegex, "");
			//logger.info("Tested :" + newChar);
			count++;
		}
		//logger.info("Added {} lines of special characters to be stripped from text.", lines.length);
		sb.append("]");

		SPECIAL_CHARS_REGEX = sb.toString();

		//logger.info("Regex for special characters : {}", SPECIAL_CHARS_REGEX);

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
	public static String removePunctuationForAnalysis(String input) {
		//String filterExp1 = "?|#|\\$|=|&|%|\\*|\\+|\\-|\\[|\\]";
		String filterExp1 = "\\?|\\.|\\!|;|:";
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
					//logger.info("Skipping line {} as it lead to empty string ..", lineCount);
				} else {
					if (!langs.contains(newLine)) {
						words.add(newLine);
					} else {
						//logger.info("Skipping line {} as it is an English word {} ..", lineCount, newLine);
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
	public static String extractSTCFromText(String text) {
		String step1 = text.replaceAll("\\!”", "\\!”\r\n");
		String step2 = step1.replaceAll("\\?”","\\?”\r\n");
		String step3 = step2.replaceAll("\\.”","\\.”\r\n");
		String step4 = step3.replaceAll("\\. ",". \r\n");

		String step5 = step4.replace("…","\r\n");
		
        Pattern pattern1 = Pattern.compile("\\(([^)]*)\\)");
        Matcher matcher1 = pattern1.matcher(step5);

        StringBuffer result1 = new StringBuffer();

        // Find all occurrences of text between parentheses
        while (matcher1.find()) {
            // Replace newlines (\r or \n) in the content between parentheses
            String cleanedContent = matcher1.group(1).replaceAll("[\\r\\n]", "");
            // Escape the parentheses to prevent illegal group reference errors
            matcher1.appendReplacement(result1, Matcher.quoteReplacement("(" + cleanedContent + ") "));
        }
        matcher1.appendTail(result1); // Append the rest of the string
        
        String step6 = result1.toString();
        StringBuilder modifiedText = new StringBuilder(step6);

        int index = 0;
        while ((index = modifiedText.indexOf("”\r\n", index)) != -1) {
            // Look for "dedi" or "diye" after "”\r\n"
            int dediIndex = modifiedText.indexOf("dedi", index + 3);
            int diyeIndex = modifiedText.indexOf("diye", index + 3);
            
            // Find the next period
            int periodIndex = modifiedText.indexOf(".", index + 3);

            // Check if "dedi" or "diye" appears before the next period
            if ((dediIndex != -1 && dediIndex < periodIndex) || (diyeIndex != -1 && diyeIndex < periodIndex)) {
                // Remove \r\n
                modifiedText.delete(index + 1, index + 3);
                // Update index to reflect the removal
                index += 2;
            } else {
                // Move to the next position if no match is found
                index += 3;
            }
        }

        String step10 = modifiedText.toString();
		//String step10 = pretext.replaceAll("”", "”\r\n");
		String step11 = step10.replaceAll("\r\n\r\n", "\r\n");
		//String step11 = step10.replaceAll("\\n\\r\\n", "\\r\\n");

        return step11;
       
	}
}