package com.langpack.integration;

public class TDKMeaningItem {

	public TDKMeaningItem() {

	}

	public TDKMeaningItem(String _word) {
		word = _word;
	}

	public TDKMeaningItem(String word, String chapter, String meaningId, String wordType, String meaning, String sample,
			String sampleAuthor) {
		this.word = word;
		this.chapterId = chapter;
		this.meaningId = meaningId;
		this.wordType = wordType;
		this.meaning = meaning;
		this.sample = sample;
		this.sampleAuthor = sampleAuthor;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getChapterId() {
		return chapterId;
	}

	public void setChapterId(String chapter) {
		this.chapterId = chapter;
	}

	public String getMeaningId() {
		return meaningId;
	}

	public void setMeaningId(String meaningId) {
		this.meaningId = meaningId;
	}

	public String getMeaning() {
		return meaning;
	}

	public void setMeaning(String meaning) {
		this.meaning = meaning;
	}

	public String getWordType() {
		return wordType;
	}

	public void setWordType(String wordType) {
		this.wordType = wordType;
	}

	public String getSample() {
		return sample;
	}

	public void setSample(String sample) {
		this.sample = sample;
	}

	public String getSampleAuthor() {
		return sampleAuthor;
	}

	public void setSampleAuthor(String sampleAuthor) {
		this.sampleAuthor = sampleAuthor;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public String getCombinations() {
		return combinations;
	}

	public void setCombinations(String combinations) {
		this.combinations = combinations;
	}

	public String getLangContent() {
		return langContent;
	}

	public void setLangContent(String langContent) {
		this.langContent = langContent;
	}

	public String getChapterName() {
		return chapterName;
	}

	public void setChapterName(String chapterName) {
		this.chapterName = chapterName;
	}

	@Override
	public String toString() {
		return "TDKMeaningItem [id=" + id + ", word=" + word + ", chapterId=" + chapterId + ", chapterName="
				+ chapterName + ", meaningId=" + meaningId + ", meaning=" + meaning + ", wordType=" + wordType
				+ ", sample=" + sample + ", sampleAuthor=" + sampleAuthor + ", langCode=" + langCode + ", langContent="
				+ langContent + ", combinations=" + combinations + "]";
	}

	String id;
	String word;
	String chapterId;
	String chapterName;
	String meaningId;
	String meaning;
	String wordType;
	String sample;
	String sampleAuthor;
	String langCode;
	String langContent;
	String combinations;
}
