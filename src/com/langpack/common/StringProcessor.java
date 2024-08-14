package com.langpack.common;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringProcessor {

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

		return step7;
	}

	public static ArrayList<String> convertToKeywords(String input) {

		String filterRegex = "\\s*|\\.|\\'|\\d*|,|!"; 
		
		String[] rawWords = input.split(filterRegex); 

		ArrayList<String> filteredWords = new ArrayList<String>();
		
		// drop invaluable data
		for (int i = 0; i < rawWords.length; i++) {
			String tmpWord = rawWords[i];
			if (!tmpWord.matches(filterRegex)) {
				filteredWords.add(tmpWord);
			}
			
		}
		return filteredWords;
	}

}
