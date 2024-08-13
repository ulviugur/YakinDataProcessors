package com.langpack.scraper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import com.langpack.common.TextFileReader;

public class TokenList {
	static TreeSet<String> tagList = new TreeSet<>();

	public static boolean isToken(String test) {
		if (tagList.contains(test)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getTokenArrayForRegex() {
		if (tagList.size() == 0) {
			System.out.println("Token list is not initialized. size is zero, exitting ..");
			System.exit(-1);
		}
		String retval = "(";
		for (String token : tagList) {
			retval += token + "|";
		}
		if ("|".equals(retval.substring(retval.length() - 1))) {
			retval = retval.substring(0, retval.length() - 1);
		}
		retval += ")";

		return retval;
	}

	public static void readTagsFromTextFile(String fileName) {
		File file = new File(fileName);
		readTagsFromTextFile(file);
	}

	public static void readTagsFromTextFile(File file) {
		try {
			TextFileReader reader = new TextFileReader(file);

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!"".equals(line)) {
					tagList.add(line);
				}
			}
			line = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String removeTokens(String iniValue) {
		String value = iniValue;
		String token = null;
		Iterator<String> iter = tagList.iterator();
		try {
			while (iter.hasNext()) {
				token = iter.next();
				// System.out.println("Token_" + token + "_");
				// System.out.println("_" + value + "_");
				value = value.replaceAll(token, "");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return value;
	}

	public static TreeSet<String> removeTokens(TreeSet<String> inList) {
		TreeSet<String> outList = new TreeSet<>();

		for (String item : inList) {
			String item2 = removeTokens(item);
			if ("".equals(item2)) {
				// System.out.println("Replaced _" + item + "_");
			} else {
				outList.add(item2);
			}
		}
		return outList;
	}
}
