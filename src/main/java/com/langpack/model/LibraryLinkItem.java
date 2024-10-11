package com.langpack.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryLinkItem {

	private String keysCombi;
	private String scrapeURL;
	private String bookName;
	private String author;
	private ArrayList<String> links;

}
