package com.langpack.integration;

import java.util.Locale;
import java.util.TreeSet;

public class WikiLevel2Parser extends Level2Parser {

	public WikiLevel2Parser() {

	}

	@Override
	public String[] getWordsAsArray() {
		String[] retval = null;
		if (level2 == null) {
			return retval;
		} else {
			retval = level2.split("\\b");
			return retval;
		}
	}

	@Override
	public TreeSet<String> getWordsAsCollection() {
		TreeSet<String> retval = new TreeSet<>();
		Locale locale = new Locale("tr", "TR");
		String[] words = getWordsAsArray();
		for (String word2 : words) {
			String word = word2;
			word = word.trim();
			word = word.replaceAll("\\s+", "");
			word = word.toLowerCase(locale);
			retval.add(word);
		}
		log4j.info(retval);
		return retval;
	}

	@Override
	public String removeUnwantedKeywords(String inStr) {
		String retval = inStr;
		for (String key : KEYS) {
			retval = retval.replaceAll(key, " ");
		}
		return retval;
	}

	@Override
	public TreeSet<String> postProcess(TreeSet<String> inArray) {
		TreeSet<String> retval = new TreeSet<>();

		for (String phrase : inArray) {//
			String newPhrase = phrase;
			if (newPhrase.contains("_")) {
				newPhrase = phrase.replace("_", " ");
			} else if ("–".equals(phrase) || "-".equals(phrase) || "’".equals(phrase)) {
				newPhrase = null;
			}
			if (newPhrase != null && newPhrase.trim().length() == 0) {
				newPhrase = null;
			}

			if (newPhrase != null) {
				retval.add(newPhrase);
			}
		}
		return retval;
	}

	@Override
	public String getLevel2Word() {
		String retval = null;
		return retval;
	}
}
