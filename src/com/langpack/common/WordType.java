package com.langpack.common;

public enum WordType {
	EXACT_MATCH, // Yes, this is a Turkish word and it's in the Zemberek library
	POSSIBLE_MATCH, // It's not in the Zemberek library as it is, but it resembles another word
	UNRECOGNIZED_WORD, // No, this is not a Turkish word at all
}
