package com.langpack.common;

import java.util.ArrayList;
import java.util.HashMap;

public class LevensteinIndex {
	private static Double SIMILAR_ENOUGH = 0.85;

	public static int distance(String a, String b) {
		a = a.toLowerCase();
		b = b.toLowerCase();
		// i == 0
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j < costs.length; j++) {
			costs[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			// j == 0; nw = lev(i - 1, j)
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
						a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}

		return costs[b.length()];
	}

	public static HashMap<Integer, ArrayList<String>> getFirstDifference(String a, String b) {
		String A = a.toUpperCase();
		String B = b.toUpperCase();
		HashMap<Integer, ArrayList<String>> retval = new HashMap<>();
		ArrayList<String> diffLetters = new ArrayList<>();
		Integer maxLength = Math.max(a.length(), b.length());
		for (int i = 0; i < maxLength; i++) {
			String strA = A.substring(i, i + 1);
			String strB = B.substring(i, i + 1);
			if (!strA.equals(strB)) {
				diffLetters.add(strA);
				diffLetters.add(strB);
				retval.put(i, diffLetters);
				return retval;
			} else {
				if (a.length() > b.length() && i + 1 == b.length()) {
					strA = A.substring(i + 1, i + 2);
					diffLetters.add(strA);
					diffLetters.add("");
					retval.put(i + 1, diffLetters);
					return retval;
				} else if (b.length() > a.length() && i + 1 == a.length()) {
					strB = B.substring(i + 1, i + 2);
					diffLetters.add("");
					diffLetters.add(strB);
					retval.put(i + 1, diffLetters);
					return retval;
				}
			}

		}
		return retval;
	}

	public static boolean similar(String a, String b, Double barrier) {

		if (barrier == null) {
			barrier = SIMILAR_ENOUGH;
		}
		int dist = distance(a, b);
		double relative = 1.0 - ((double) dist) / (a.length() + b.length());
		/*
		 * if (a.startsWith("KIA") && b.startsWith("KIYA")) {
		 * System.out.println("Distance " + a + "::" + b + "=" + relative); }
		 */
		if (relative > barrier) {
			return true;
		} else {
			return false;
		}
	}

	// Catch the best keyword and check if it is similar enough
	public static String bestSimilar(String ref, String searching, Double barrier) {
		String retval = null;
		String[] refArray = ref.split(GlobalUtils.REGEX_WORD_BREAKER);

		// if the searched key is multiple words, we need to search for them seperately
		String[] searchArray = searching.split(GlobalUtils.REGEX_WORD_BREAKER);

		int countWords = searchArray.length;
		ArrayList<String> foundKeys = new ArrayList<>();
		for (String element : searchArray) {
			String key = element;
			key = key.replaceAll(",", "");
			for (String element2 : refArray) {
				String refKey = element2;
				refKey = refKey.replaceAll(",", "");
				boolean tmp = similar(key, refKey, barrier);
				if (tmp) {
					key = GlobalUtils.normalizeUnicodeStringLeaveCaseAsIs(element2);
					if (!foundKeys.contains(key)) {
						foundKeys.add(key);
						break;
					}
				}
			}
		}
		if (foundKeys.size() == countWords) {
			retval = "";
			for (String foundKey : foundKeys) {
				retval = retval + foundKey + " ";
			}
			retval = retval.trim();
		}
		return retval;
	}

	public static double relativeIndex(String a, String b) {
		if (a == null || b == null) {
			return 0;
		}
		int dist = distance(a, b);
		double relative = 1.0 - ((double) dist) / (a.length() + b.length());
		return relative;
	}

	public static void main(String[] args) {
		String[] data = { "Ghazzala", "Gazala" };
		for (int i = 0; i < data.length; i += 2) {
			System.out.println("distance(" + data[i] + ", " + data[i + 1] + ") = " + distance(data[i], data[i + 1]));
			System.out.println(
					"relativeDistance(" + data[i] + ", " + data[i + 1] + ") = " + relativeIndex(data[i], data[i + 1]));
		}
	}
}