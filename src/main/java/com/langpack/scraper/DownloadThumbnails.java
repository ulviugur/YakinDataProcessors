package com.langpack.scraper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.langpack.common.ConfigReader;
import com.langpack.datachannel.DataChannelFactory;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.process.golddata.MergeBooksData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;

public class DownloadThumbnails {

	public static final Logger log4j = LogManager.getLogger("DNRBooksLookup");

	String destFolderStr = null;
	String booksCollStr = null;

	protected ConfigReader cfg = null;

	ArrayList<String> bookKeys = new ArrayList<String>();

	String mongoURL = null;
	MongoCollection<Document> booksColl = null;
	// MongoCollection<Document> linksColl = null;
	
	MongoDatabase database = null;
	MongoClient mongoClient = null;

	// HashSet<String> existingKeys = new HashSet<String>();

	public DownloadThumbnails(String cfgFileName) throws UnknownDataChannelException {
		cfg = new ConfigReader(cfgFileName);
		destFolderStr = cfg.getValue("DestinationFolder");
		booksCollStr = cfg.getValue("BooksCollection");
		mongoURL = cfg.getValue("MongoURL", "mongodb://localhost:27017");

		DataChannelFactory.initialize(cfgFileName);

		mongoClient = MongoClients.create(mongoURL);
		database = mongoClient.getDatabase("test");
		
		booksColl = database.getCollection(booksCollStr);
		// linksColl = database.getCollection(linksCollStr);
	}

	public void process() {
		int runfor = 11000;
		// Run the find() query with the filter
		MongoIterable<Document> result = booksColl.find();

		// Iterate over the results
		MongoCursor<Document> cursor = result.iterator();
		int count = 0;
		try {
			while (cursor.hasNext()) {

				Document doc = cursor.next();
				log4j.info("[{}] Running for {}", count, doc);

				String thumbnailURL1 = (String) doc.get("thumbnailURL");
				String thumbnailURL = thumbnailURL1.replace("size:96", "size:640");

				String localFileName = MergeBooksData.makeLocalTNFilename(doc);
				
				File localFile = new File(destFolderStr, localFileName);
				log4j.info("[{}] Download URL {} and LocalFile : {}", count, thumbnailURL, localFile.getAbsolutePath());

				String currLocalTNFilename = doc.getString("localTNFilename");
				if (localFileName.equals(currLocalTNFilename)) {
					// filename already updated
				} else {
					// create TN field
					Document tnField = new Document("localTNFilename", localFileName);
				    Document updateQuery = new Document("$set", tnField);

				    // use a filter to get the book document to be update - which is actually the same doc
					Document filter = new Document("_id", doc.get("_id"));
				    UpdateResult res = booksColl.updateOne(filter, updateQuery);
				    log4j.info("Modify {} documents : ", res.getModifiedCount());
				    
				}
								
				boolean success = downloadImage(thumbnailURL, localFile, false);

				count++;
				runfor--;
				if (runfor == 0) {
					break;
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			cursor.close();
		}
		log4j.info("Process completed after {} downloads ..", count);
	}
	public static Document processThumbnail (Document doc, File TNDestFolder) {
		String thumbnailURL1 = (String) doc.get("thumbnailURL");
		String thumbnailURL = thumbnailURL1.replace("size:96", "size:640");

		String localFileName = MergeBooksData.makeLocalTNFilename(doc);
		log4j.info(localFileName);
		
		File localFile = new File(TNDestFolder, localFileName);

		if (localFile.exists()) {
			// TN file already exists
			return doc;
		} else {			
			log4j.info("Download URL {} and LocalFile : {}",  thumbnailURL, localFile.getAbsolutePath());
			
			boolean success = downloadImage(thumbnailURL, localFile, false);
			if (success) {
				doc.put("localTNFilename", localFileName);
			}
			return doc;
		}
	}

	public static boolean downloadImage(String imageUrl, File destFile, boolean overwrite) {
		Path destinationPath = null;

		if (destFile.exists() && !overwrite) {
		
			log4j.info("File {} already exist, will not overwrite, skipping ..", destFile.getAbsolutePath());
			return false;
		}
		
		try {
			destinationPath = Paths.get(destFile.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try (InputStream inputStream = new URL(imageUrl).openStream()) {

			Files.copy(inputStream, destinationPath);
			log4j.info("Image downloaded successfully: " + destinationPath);
			try {
				Thread.sleep(1500);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			log4j.info("Failed to download image {} to {}", imageUrl, destFile.getAbsolutePath());
			return false;
		}

	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		DownloadThumbnails instance;
		try {
			instance = new DownloadThumbnails(args[0]);
			instance.process();

		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// instance.scrapeContent();

	}

}
