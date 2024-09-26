package com.langpack.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "bookquote")
public class BookQuoteData {

	@Id
	private ObjectId id; // Unique identifier for the document
	private String word;
	private String quote;
	private String bookTitle;
	private String author;
	private int publicationYear;
	private String thumbnail;
	private Date showDate;
	
}
