package com.langpack.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Data
@NoArgsConstructor
@Document(collection = "GoldQuotes")
public class BookQuote {

	@Id
	private ObjectId id; // Unique identifier for the document
	private String key;
	private String word;
	private String quote;
	private String bookTitle;
	private String author;
	private Date showDate;
	
}
