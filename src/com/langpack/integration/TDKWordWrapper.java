package com.langpack.integration;

import java.util.ArrayList;
import java.util.TreeSet;

public class TDKWordWrapper {

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public String getLangContent() {
		return langContent;
	}

	public void setLangContent(String langContent) {
		this.langContent = langContent;
	}

	public TreeSet<String> getCombinations() {
		return combinations;
	}

	public void addCombination(String combi) {
		combinations.add(combi);
	}

	public TreeSet<String> getProverbs() {
		return proverbs;
	}

	public void addProverb(String proverb) {
		proverbs.add(proverb);
	}

	public ArrayList<TDKChapterItem> getChapterItems() {
		return chapterItems;
	}

	public void addChapterItem(TDKChapterItem item) {
		chapterItems.add(item);
	}

	ArrayList<TDKChapterItem> chapterItems = new ArrayList<>();

	TreeSet<String> proverbs = new TreeSet<>();
	TreeSet<String> combinations = new TreeSet<>();

	String langCode = null;
	String langContent = null;
}
