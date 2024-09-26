package com.langpack.integration;

import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class Level2Parser {
	String level2 = null;

	static final String[] KEYS = { "class=", "tocsection-\\d", "toctext", "\"mw-editsection-bracket\"",
			"\"mw-editsection\"", "\"mw-parser-output\"", "checkbox", "role", "button", "toctogglecheckbox",
			"tocnumber=", "toclevel-\\d", "editsection", "aria-labelledby=\"mw-toc-heading\"", "toctogglespan",
			"toctogglelabel", "rel=\"nofollow", "\"toc\"", "\"toctitle\"", "toctoggle", "toclevel=", "role=", "\\s+id=",
			"mw-toc-heading", "tocnumber", "href=", "\"mw-headline\"", "style=", "background-color:#.{6};",
			"vertical-align:", "text-align:" };

	public final Logger log4j = LogManager.getLogger("Level2Parser");

	public abstract String[] getWordsAsArray();

	public abstract TreeSet<String> getWordsAsCollection();

	public abstract String removeUnwantedKeywords(String inStr);

	public abstract TreeSet<String> postProcess(TreeSet<String> inArray);

	public abstract String getLevel2Word();

	public void setLevel2(String level2) {
		char hyphen = (char) 45;
		char hyphenSoft = (char) 173;
		String sHyphen = Character.toString(hyphen);
		String sHyphenSoft = Character.toString(hyphenSoft);
		String level2_0 = level2.replaceAll(sHyphen + "\\r\\n", "");
		String level2_1 = level2_0.replaceAll(sHyphenSoft + "\\r\\n", "");
		String level2_2 = level2_1.replaceAll(sHyphen + "\\n", "");
		String level2_3 = level2_2.replaceAll(sHyphenSoft + "\\n", "");

		// Word split over dash should not create additional words (ta-\r\nşınmaksızın)
		this.level2 = level2_3;
	}

}
