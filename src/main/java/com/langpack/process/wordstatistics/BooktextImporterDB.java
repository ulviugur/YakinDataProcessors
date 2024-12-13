package com.langpack.process.wordstatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileIterator;
import com.langpack.common.FileLedger;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

// Uses a list of all book text in a specific directory
// Creates a (unique ?) list of all words in the database (oracle !)

public class BooktextImporterDB {
	public static final Logger logger = LogManager.getLogger("BooktextImporterDB");

	File sourceDir = null;
	String targetDBConn = null;

	FileIterator fiter = null;
	File currFile = null;
	
	FileLedger ledger = null;

	Connection conn = null;

	String insertQuery1 = "INSERT INTO WORD_POOL ( word, source ) VALUES ( ?, ? )";
	String selectQuery1 = "SELECT COUNT(*) FROM WORD_POOL WHERE WORD = ?";

	PreparedStatement ps1 = null;
	PreparedStatement ps2 = null;

	ArrayList<String> skippedList = new ArrayList<String>();

	public BooktextImporterDB(File sourceDir, String dbConnStr, String user, String pass, String dbDriverStr, String ledgerFileStr) {
		try {
			fiter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ledger = new FileLedger(ledgerFileStr);
			logger.info("Reading ledger file: \"{}\" ...", ledger.getLedgerFile().getAbsolutePath());
			ledger.loadLedger();
			logger.info("Found {} entries in the ledger file.", ledger.getLedgerLength());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			Class.forName(dbDriverStr);
			conn = DriverManager.getConnection(dbConnStr, user, pass);
			conn.setAutoCommit(true);
			ps1 = conn.prepareStatement(insertQuery1);
			ps2 = conn.prepareStatement(selectQuery1);
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
				boolean processed = ledger.isInledger(currSourceFile.getAbsolutePath());
				if (processed) {
					logger.info("[{}] Skipping file: \"{}\" as it was already processed !", totalCount, currSourceFile.getAbsolutePath());
					// The file was already processed, jump over
					currSourceFile = fiter.moveToNextTarget();
					totalCount++;
					continue;
				} else {
					logger.info("[{}] File: \"{}\" is not found in the ledger, processing ..", totalCount, currSourceFile.getAbsolutePath());
				}

				TextFileReader reader = new TextFileReader(currSourceFile);
				boolean opened = reader.openFile();
				String content = null;
				if (opened) {
					content = reader.readFile();
					HashSet<String> wordsArray = StringProcessor.convertToKeywords(content);
					Iterator<String> iter = wordsArray.iterator();

					int existing;
					int insertCount = 0;
					while (iter.hasNext()) {
						String word = iter.next();

						// check if the word is already in the database
						try {
							ps2.setString(1, word);
							ResultSet res = ps2.executeQuery();
							res.next();
							existing = res.getInt(1);
							if (existing == 0) {
								try {
									ps1.setString(1, word);
									ps1.setString(2, currSourceFile.getName());
									int tmp = ps1.executeUpdate();
									insertCount += tmp;
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								logger.debug("Skipping word as it already exists ! >>  \"{}\"", word);
							}
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					logger.info("[{}] Inserted {} records from \"{}\" .", totalCount, insertCount,
							currSourceFile.getName());
					ledger.addtoLedger(currSourceFile.getAbsolutePath());
				} else {
					logger.info("[{}] Source file {} could not be opened ! Skipping.", totalCount,
							currSourceFile.getAbsolutePath());
					skippedList.add(currSourceFile.getAbsolutePath());
				}

				currSourceFile = fiter.moveToNextTarget();
				totalCount++;
			}
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exitting");
			ledger.closeLedger();
			System.exit(-1);
		}
		logger.info("BooktestImport process completed ..");
		if (skippedList.size() > 0) {
			ledger.closeLedger();
			logger.warn("Skipped {} files : ", skippedList.size());
			for (int i = 0; i < skippedList.size(); i++) {
				logger.warn("\t[] Skipped file {}  ", i + 1, skippedList.get(i));
			}
		}
	}

	public static void main(String[] args) {

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String sourceDirPath = argsObject.get("--inputdir");

		String targetDBURL = argsObject.get("--dbURL");

		String dbUser = argsObject.get("--dbuser");
		String dbPass = argsObject.get("--dbpass");
		String dbDriver = argsObject.get("--dbdriver");
		
		String ledgerFilePath = argsObject.get("--ledgerfile");

		File sourceDir = new File(sourceDirPath);

		BooktextImporterDB process = new BooktextImporterDB(sourceDir, targetDBURL, dbUser, dbPass, dbDriver, ledgerFilePath);
		process.runFiles();

	}
}
