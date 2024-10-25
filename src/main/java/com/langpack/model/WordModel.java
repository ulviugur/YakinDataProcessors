package com.langpack.model;

import com.langpack.model.suffix.SuffixType;

import lombok.Data;

@Data
public class WordModel {
	private WordType type;
	private String originalWord;
	private String mappedWord;
	private String stem;
	private String rootWord;
	private SuffixType suffixType;
	private String[] suffixTypes;
	private String[] derivedWordList;
	
	public WordModel() {
        // Default constructor
    }

	public WordModel (WordType type, String originalWord, String mappedWord, String stem, String rootWord, SuffixType suffixType) {
		this.type = type;
		this.originalWord = originalWord;
		this.mappedWord = mappedWord;
		this.stem = stem;
		this.rootWord = rootWord;
		this.suffixType = suffixType;
	}

	@Override
	public String toString() {
		return "WordModel [type=" + type + ", originalWord=" + originalWord + ", mappedWord=" + mappedWord + ", stem="
				+ stem + ", rootWord=" + rootWord + ", suffixType=" + suffixType + "]";
	}



}
