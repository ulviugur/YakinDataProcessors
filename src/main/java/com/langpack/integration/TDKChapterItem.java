package com.langpack.integration;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TDKChapterItem {
	public static final Logger log4j = LogManager.getLogger("TDKChapterItem");

	public TDKChapterItem(String tmpWord) {
		setWord(tmpWord);
	}

	public ArrayList<TDKMeaningItem> getMeaningItems() {
		return meaningItems;
	}

//	public void setMeaningItems(ArrayList<TDKMeaningItem> tmpMeanings) { // meaning details are inherited to the meanings
//		for (int i = 0; i < tmpMeanings.size(); i++) {
//			TDKMeaningItem item = tmpMeanings.get(i);
//			addMeaningItem(item);
//		}
//	}
	public void addMeaningItem(TDKMeaningItem item) {
		if (getChapterId() != null) {
			item.setChapterId(getChapterId());
		}
		if (getChapterName() != null) {
			item.setChapterName(getChapterName());
		}
		item.setWord(getWord());
		meaningItems.add(item);
		for (TDKMeaningItem tmp : meaningItems) {
		}
	}

	public String getChapterId() {
		return chapterId;
	}

	public void setChapterId(String chapterId) {
		this.chapterId = chapterId;
	}

	public String getChapterName() {
		return chapterName;
	}

	public void setChapterName(String chapterName) {
		this.chapterName = chapterName;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	@Override
	public String toString() {
		return "TDKChapterItem [chapterId=" + chapterId + ", chapterName=" + chapterName + "meaningItems="
				+ meaningItems + "]";
	}

	ArrayList<TDKMeaningItem> meaningItems = new ArrayList<>();
	String chapterId = null;
	String chapterName = null;
	String word = null;
}
