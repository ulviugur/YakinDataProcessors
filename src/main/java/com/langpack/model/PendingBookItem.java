package com.langpack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingBookItem {

	private String oriFileName;
	private String found;
	
	private String cleanFileName;
	private String oriBookname;
	private String oriAuthor;

	private String bestBookName;
	private String bestAuthor;
	
	private String finalBookName;
	private String finalAuthor;
	
	private String key;
	private String keysCombi;
	private String scrapeURL;

	private String subtitle;

	private String thumbnailURL;
	
	private String metaData;
	private String publisher;
	private String publishYear;
	
	private Integer index;

}
