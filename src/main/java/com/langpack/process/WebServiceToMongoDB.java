package com.langpack.process;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class WebServiceToMongoDB {

	public static final Logger log4j = LogManager.getLogger("WebServiceToMongoDB");

	private static final String DATABASE_NAME = "test";
	private static final String TARGET_COLL_NAME = "tdk_raw";
	private static final String WORD_COLL_NAME = "words";

	public static final String TDK_BASE_URL = "https://sozluk.gov.tr/gts";
	MongoClient mongoClient = null;
	MongoDatabase database = null;
	MongoCollection<Document> targetColl = null;
	MongoCollection<Document> wordColl = null;

	TreeSet<String> wordSet = new TreeSet<String>();

	private String getPullURL(String word) {
		String retval = TDK_BASE_URL + "?ara=" + word;
		return retval;
	}

	public WebServiceToMongoDB() {
		   MongoClient mongoClient = MongoClients.create(String.format("mongodb://%s:%s", "localhost", "27017"));
		MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
		wordColl = database.getCollection(WORD_COLL_NAME);
		targetColl = database.getCollection(TARGET_COLL_NAME);
	}

	public MongoIterable<Document> selectUnfilledRecords() {
		List<Bson> pipeline = Arrays.asList(Aggregates.lookup("tdk_raw", // Collection to join with
				"word", // Field from the `words` collection
				"word", // Field from the `tdk_raw` collection
				"data" // Name of the new array field to store the joined data
		), Aggregates.match(Filters.expr(new Document("$eq", Arrays.asList(new Document("$size", "$data"), 0)))));

		// Run the aggregation query
		MongoIterable<Document> results = wordColl.aggregate(pipeline);

		return results;
	}

	public TreeSet<String> readAllKeysFromMongo() {
		MongoCursor<Document> cursor = wordColl.find().iterator();
		try {
			int count = 1;
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String word = doc.getString("word");
				wordSet.add(word);
				if ((wordSet.size() % 100 == 0) || (count % 1000 == 0)) {
					log4j.info("Loaded {} words ", wordSet.size());
				}
				count++;
			}
			log4j.info("Loaded {} words ", wordSet.size());

		} finally {
			cursor.close();
		}

		return wordSet;
	}

	public void collectandInsertData(String word) {
		String webURL = getPullURL(word);

		try {

			String jsonData = fetchJSONFromWebService(webURL);

			Document document = new Document().append("word", word) // Add your specific word here
					.append("data", jsonData); // Store the full JSON data as a string

			// Step 4: Insert the Document into MongoDB
			targetColl.insertOne(document);

			log4j.info("{} Data inserted successfully into MongoDB!", word);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void process() {

		// MongoIterable<Document> orphans = selectUnfilledRecords();
		// MongoCursor<Document> iter = orphans.iterator();

		readAllKeysFromMongo();
		log4j.info("Found {} records in words collection ..", wordSet.size());
		
		Iterator<String> iter = wordSet.iterator();
		
		int count = 1;
		while (iter.hasNext()) {
			String word = iter.next();
			log4j.info("{} -> {}", count, word);
			count++;
		}

		/*
		 * readAllKeysFromMongo();
		 * 
		 * Iterator<String> iter = wordSet.iterator(); while (iter.hasNext()) { String
		 * key = iter.next(); collectandInsertData(key); }
		 */

		mongoClient.close();
	}

	public static void main(String[] args) {

		WebServiceToMongoDB runner = new WebServiceToMongoDB();
		runner.process();

	}

	/**
	 * Fetches JSON data from a web service using HttpURLConnection.
	 * 
	 * @param webServiceUrl the URL of the web service.
	 * @return the JSON data as a string.
	 * @throws Exception in case of any issues with the connection.
	 */
	private static String fetchJSONFromWebService(String webServiceUrl) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(webServiceUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");

		try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		}

		return result.toString();
	}

}
