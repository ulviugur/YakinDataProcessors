package com.langpack.process;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileIterator;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

public class ImportDocumentFiles {
	public static final Logger logger = LogManager.getLogger("ImportDocumentFiles");

	File sourceDir = null;
	String targetDBConn = null;
	String dbUser = null;
	String dbPass = null;

	FileIterator fiter = null;
	File currFile = null;

	Connection conn = null;
	
	String insertQuery1 = "INSERT INTO WORDS_POOL ( word ) VALUES ( ? )";
	PreparedStatement ps1 = null;
	
	ArrayList<String> skippedList = new ArrayList<String>();

	public ImportDocumentFiles(File sourceDir, String dbConnStr, String user, String pass, String dbDriverStr) {
		try {
			fiter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Class.forName(dbDriverStr);
			conn = DriverManager.getConnection(dbConnStr, dbUser, dbPass);
			conn.setAutoCommit(true);
			ps1 = conn.prepareStatement(insertQuery1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runFiles() {
		int totalCount = 0;

		File currSourceFile = fiter.getCurrFile();

		try {
			while (currSourceFile != null) {

				logger.info("[{}] Current source file: \"{}\"", totalCount, currSourceFile.getAbsolutePath());
				
				TextFileReader reader = new TextFileReader(currSourceFile);
				boolean opened = reader.openFile();
				String content = null;
				if (opened) {
					content = reader.readFile();
					ArrayList<String> wordsArray = StringProcessor.convertToKeywords(content);
					for (int i = 0; i < wordsArray.size(); i++) {
						String word = wordsArray.get(i);
						try {
							ps1.setString(1, word);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					logger.info("[{}] Source file {} could not be opened ! Skipping.", totalCount, currSourceFile.getAbsolutePath());
					skippedList.add(currSourceFile.getAbsolutePath());
				}

				currSourceFile = fiter.moveToNextTarget();
				totalCount++;
			}
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exitting");
			System.exit(-1);
		}
	}

	public static void main(String[] args) {

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String sourceDirPath = argsObject.get("--inputdir");

		String targetDBURL = argsObject.get("--dbURL");

		String dbUser = argsObject.get("--dbuser");
		String dbPass = argsObject.get("--dbpass");
		String dbDriver = argsObject.get("--dbdriver");

		File sourceDir = new File(sourceDirPath);

		ImportDocumentFiles process = new ImportDocumentFiles(sourceDir, targetDBURL, dbUser, dbPass, dbDriver);

	}
}
