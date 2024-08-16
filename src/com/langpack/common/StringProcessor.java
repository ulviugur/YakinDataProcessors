package com.langpack.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class StringProcessor {
	public static Locale turkishLocale = new Locale.Builder().setLanguage("tr").setRegion("TR").build();

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

}


