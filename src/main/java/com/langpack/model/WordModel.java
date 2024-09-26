package com.langpack.model;

public class WordModel {
	private WordType type;
	private String originalWord;
	private String mappedWord;
	private String stem;
	private String rootWord;
	
	public WordModel() {
        // Default constructor
    }

	public WordModel (WordType type, String originalWord, String mappedWord, String stem, String rootWord) {
		this.type = type;
		this.originalWord = originalWord;
		this.mappedWord = mappedWord;
		this.stem = stem;
		this.rootWord = rootWord;
	}
	
	public WordType getType() {
		return type;
	}

	public void setType(WordType type) {
		this.type = type;
	}

	public String getOriginalWord() {
		return originalWord;
	}

	public void setOriginalWord(String originalWord) {
		this.originalWord = originalWord;
	}

	public String getMappedWord() {
		return mappedWord;
	}

	public void setMappedWord(String mappedWord) {
		this.mappedWord = mappedWord;
	}

	public String getStem() {
		return stem;
	}

	public void setStem(String stem) {
		this.stem = stem;
	}
	
	public String getRootWord() {
		return rootWord;
	}

	public void setRootWord(String rootWord) {
		this.rootWord = rootWord;
	}

	@Override
	public String toString() {
		return "WordCheck{" +
				"type=" + type +
				", originalWord='" +originalWord + '\'' +
				", mappedWord='" + mappedWord + '\'' +
                ", stem='" + stem + '\'' +
                '}';
	}



}
