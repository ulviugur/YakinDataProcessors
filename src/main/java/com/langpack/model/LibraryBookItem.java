package com.langpack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryBookItem {

	private String keysCombi;
	private String scrapeURL;
	private String bookName;
	private String author;
	private String thumbnailURL;
	
	private String metaData;
	private String publisher;
	private String publishYear;
	
	private Integer index;

}
