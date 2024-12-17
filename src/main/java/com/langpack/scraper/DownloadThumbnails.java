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

	public static final Logger log4j = LogManager.getLogger("DownloadThumbnails");

	public static Document processThumbnail (Document doc, File TNDestFolder) {
		String thumbnailURL1 = (String) doc.get("thumbnailURL");
		String thumbnailURL = thumbnailURL1.replace("size:96", "size:640");

		String localFileName = MergeBooksData.makeLocalTNFilename(doc);
		//log4j.info(localFileName);
		
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

	private static boolean downloadImage(String imageUrl, File destFile, boolean overwrite) {
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

}
