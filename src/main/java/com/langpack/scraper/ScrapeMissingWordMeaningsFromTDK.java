package com.langpack.scraper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langpack.common.GlobalUtils;
//import com.langpack.model.BookQuoteData;
import com.langpack.model.YakinMeaningItem;
import com.langpack.process.wordstatistics.FileBasedBookContentProcessing;
import com.langpack.structure.SyllableBreaker;
import com.langpack.tdk.Anlam;
import com.langpack.tdk.Madde;
import com.langpack.tdk.Ozellik;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

public class ScrapeMissingWordMeaningsFromTDK {
	public static final Logger log4j = LogManager.getLogger("FindWordsWithoutMeaning");

	String mongoURL = "mongodb://localhost:27017";
	MongoCollection<Document> wordsCollection = null;
	MongoCollection<Document> tdkCollection = null;
	MongoCollection<Document> newmCollection = null;
	MongoCollection<Document> meaningsCollection = null;

	public void runForSelectedRecords() {
		// Create a connection to the MongoDB server
		try (var mongoClient = MongoClients.create()) {

			// Connect to the database and collection
			MongoDatabase database = mongoClient.getDatabase("test");
			MongoCollection<Document> wordsCollection = database.getCollection("words");

			// Build the aggregation pipeline
			List<Bson> pipeline = Arrays.asList(Aggregates.lookup("meanings", // Collection to join (meanings)
					"word", // Local field from `words` collection
					"word", // Foreign field from `meanings` collection
					"meaningData" // Output array field
			), Aggregates.match(Filters.size("meaningData", 0) // Match documents where `meaningData` array size is 0
			));

			// Run the aggregation query
			MongoIterable<Document> results = wordsCollection.aggregate(pipeline);

			// Print out the results
			for (Document doc : results) {
				String word = (String) doc.get("word");
			}
		}
	}

	public void runForAllWordsonTDK() {
		// Create a connection to the MongoDB server
		try (var mongoClient = MongoClients.create()) {

			// Connect to the database and collection
			MongoDatabase database = mongoClient.getDatabase("test");
			wordsCollection = database.getCollection("words");
			newmCollection = database.getCollection("newm");
			// Run the aggregation query
			MongoIterable<Document> results = wordsCollection.find();

			int count = 0;
			// Print out the results
			for (Document doc : results) {
				count++;
				String word = (String) doc.get("word");
				log4j.info("[{}] Word : {} ", count, word);
				List<Madde> items = null;
				try {
					items = TDKAPIFetcher.fetchJsonFromTDK(word);
					
					if(items == null) {
						continue;
					}

					int chapter_id = 1;
					for (Iterator<Madde> iterator = items.iterator(); iterator.hasNext();) {
						Madde madde = (Madde) iterator.next();
						String chapterName = madde.getMadde();
						String cleanChapterName = FileBasedBookContentProcessing.cleanWord(chapterName);
						YakinMeaningItem newRecord = new YakinMeaningItem();
						List<Anlam> meanings = madde.getAnlamlarListe();

						for (int i = 0; i < meanings.size(); i++) {
							Anlam meaning = meanings.get(i);
							
							newRecord.setChapter_id(Integer.toString(chapter_id));
							newRecord.setImport_time("TEST");
							newRecord.setChapter_name(cleanChapterName);
							newRecord.setCombi_type("");
							newRecord.setLang_code(madde.getLisan_kodu());
							newRecord.setLang_content(madde.getLisan());
							newRecord.setMark_as_deleted("");
							newRecord.setMeaning(meaning.getAnlam());
							newRecord.setMeaning_id(Integer.toString(i + 1));
							newRecord.setLinked_combis(madde.getBirlesikler());

							ArrayList<String> sylsArray = SyllableBreaker.break2Syls(cleanChapterName);
							String sylsStr = SyllableBreaker.convertSylArrayToString(sylsArray, SyllableBreaker.SYL_SEPARATOR);
							
							if (madde.getMadde().equals("Abaza peyniri")) {
								log4j.info("");
							}
							
							newRecord.setSyls(sylsStr);
							newRecord.setWord(madde.getMadde());
							newRecord.setWord_type("");

							String types = collectAttributesFromMeaning(meaning);
							newRecord.setWord_type2(types);

							insertOneQuoteRecord(newRecord);
						}

						chapter_id++;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}
	

	public void runForAllWordsonMongoCopy() {
		// Create a connection to the MongoDB server
		try (var mongoClient = MongoClients.create()) {

			// Connect to the database and collection
			MongoDatabase database = mongoClient.getDatabase("test");
			tdkCollection = database.getCollection("tdk_raw");
			newmCollection = database.getCollection("newm");
			// Run the aggregation query
			MongoIterable<Document> results = tdkCollection.find();

			int count = 0;
			// Print out the results
			for (Document doc : results) {
				count++;
				String word = (String) doc.get("word");
				String data = (String) doc.get("data");
				
				log4j.info("[{}] Word : {} ", count, word);
				
	            ObjectMapper objectMapper = new ObjectMapper();
	            List<Madde> items = null;
	
	            
				try {
					items = objectMapper.readValue(data, new TypeReference<List<Madde>>() {});
					if(items == null) {
						continue;
					}

					int chapter_id = 1;
					for (Iterator<Madde> iterator = items.iterator(); iterator.hasNext();) {
						Madde madde = (Madde) iterator.next();
						String chapterName = madde.getMadde();
						String cleanChapterName = FileBasedBookContentProcessing.cleanWord(chapterName);
						YakinMeaningItem newRecord = new YakinMeaningItem();
						List<Anlam> meanings = madde.getAnlamlarListe();

						for (int i = 0; i < meanings.size(); i++) {
							Anlam meaning = meanings.get(i);
							
							newRecord.setChapter_id(Integer.toString(chapter_id));
							newRecord.setImport_time("TEST");
							newRecord.setChapter_name(cleanChapterName);
							newRecord.setCombi_type("");
							newRecord.setLang_code(madde.getLisan_kodu());
							newRecord.setLang_content(madde.getLisan());
							newRecord.setMark_as_deleted("");
							newRecord.setMeaning(meaning.getAnlam());
							newRecord.setMeaning_id(Integer.toString(i + 1));
							newRecord.setLinked_combis(madde.getBirlesikler());

							ArrayList<String> sylsArray = SyllableBreaker.break2Syls(cleanChapterName);
							String sylsStr = SyllableBreaker.convertSylArrayToString(sylsArray, SyllableBreaker.SYL_SEPARATOR);
	
							newRecord.setSyls(sylsStr);
							newRecord.setWord(madde.getMadde());
							newRecord.setWord_type("");

							String types = collectAttributesFromMeaning(meaning);
							newRecord.setWord_type2(types);

							insertOneQuoteRecord(newRecord);
						}

						chapter_id++;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

			}
		}
	}

	private String collectAttributesFromMeaning(Anlam tmp) {
		String retval = "";
		List<String> typeList = new ArrayList<String>();
		List<Ozellik> attrs = tmp.getOzelliklerListe();
		if (attrs != null) {
			for (int j = 0; j < attrs.size(); j++) {
				Ozellik res = attrs.get(j);
				typeList.add(res.getTam_adi());
			}
		}

		if (typeList.size() > 0) {
			retval = GlobalUtils.convertArrayToString(typeList, ",");
		}

		return retval;
	}

	public void insertOneQuoteRecord(YakinMeaningItem data) {

		Document record = convertToDocument(data);
		newmCollection.insertOne(record);
	}

	public org.bson.Document convertToDocument(YakinMeaningItem data) {
		org.bson.Document document = new org.bson.Document(); // Create a new BSON Document
		for (Field field : YakinMeaningItem.class.getDeclaredFields()) {
			field.setAccessible(true); // Allow access to private fields
			try {
				Object value = field.get(data);
				if (value != null) {
					document.append(field.getName(), value); // Populate the Document with fields
				}
			} catch (IllegalAccessException e) {
				System.err.println("Error accessing field: " + e.getMessage());
			}
		}
		return document; // Return the BSON Document
	}

	public static void main(String[] args) {
		ArrayList<String> sylsArray = SyllableBreaker.break2Syls("Abaza peyniri");

		ScrapeMissingWordMeaningsFromTDK process = new ScrapeMissingWordMeaningsFromTDK();
		// process.runForAllWords();
		process.runForAllWordsonMongoCopy();
	}
}
