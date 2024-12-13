package com.langpack.scraper;

import org.bson.Document;

public interface BookScraper {
	 Document scrapeBookData(String scrapeURL, String bookName, String author, Integer index);
	 Document collectBookLinks(String bookName, String author);
}
