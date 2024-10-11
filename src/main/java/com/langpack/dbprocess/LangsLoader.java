package com.langpack.dbprocess;

import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class LangsLoader {
    public static TreeSet<String> loadLangs(String server, int serverPort, String dbName, String collName, String lang) {
    	TreeSet<String> retval = new TreeSet<String>();
        // Create a new MongoClient instance (adjust host and port if needed)
        MongoClient mongoClient = MongoClients.create(String.format("mongodb://%s:%s", server, serverPort));
        //MongoClient(server, serverPort);
        
        // Connect to the database (replace "yourDatabase" with your database name)
        MongoDatabase database = mongoClient.getDatabase(dbName);
        
        // Get the collection (replace "yourCollection" with your collection name)
        MongoCollection<Document> collection = database.getCollection(collName);
        
        // Create a query to find documents where lang = 'EN'
        Document query = new Document("lang", lang);
        
        // Create a list to store the lang_word values
        
        // Find all documents matching the query
        MongoCursor<Document> cursor = collection.find(query).iterator();
        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String langWord = doc.getString("lang_word");
                if (langWord != null) {
                    retval.add(langWord);
                }
            }
        } finally {
            cursor.close();
        }
        
        // Close the MongoClient
        mongoClient.close();
        
        return retval;
    }
}
