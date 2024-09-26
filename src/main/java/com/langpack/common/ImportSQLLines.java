package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImportSQLLines {
	Connection conn = null;
	// static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	// static final String DB_URL = "jdbc:mysql://U8004452-TPL-B:3306/Japan";

	static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
	static final String DB_URL = "jdbc:oracle:thin:@ACER1:1521:ACER1";
	static final String DB_USER = "master";
	static final String DB_PASS = "master";
	static final Charset cset = Charset.forName("UTF-8");
	static final boolean removeSemicolons = true;

	static int field_id = 0;

	// private static final Logger logger = LogManager.getLogger("HelloWorld");

	public static final Logger log4j = LogManager.getLogger("ImportFiles");

	public ImportSQLLines() {
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {

			Properties props = new Properties();
			props.setProperty("user", DB_USER);
			props.setProperty("password", DB_PASS);
			props.setProperty("charset", "utf8");
			conn = DriverManager.getConnection(DB_URL, props);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log4j.fatal("Connection could not be established !");
		}

	}

	public static void main(String[] args) {
		log4j.info("Staring data import ..");
		String inputFileName = args[0];
		// TODO Auto-generated method stub
		ImportSQLLines importfiles = new ImportSQLLines();
		importfiles.processFile(inputFileName);
	}

	public void processFile(String fileName) {
		log4j.info("processFile started ..");

		int fileReadCount = 0;
		int insertCount = 0;
		File inputFile = new File(fileName);

		TextFileReader reader = new TextFileReader(inputFile, "UTF8");

		String line = null;
		Statement st1 = null;

		try {
			while ((line = reader.readLine()) != null) {

				if (removeSemicolons) {
					line = line.replace(";", "");
				}
				fileReadCount++;
				log4j.info(String.format("Line [%s] : %s", fileReadCount, line));
				try {
					st1 = conn.createStatement();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					// execute insert SQL statement
					st1.executeQuery(line);
					st1.close();
					conn.commit();
					insertCount++;
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					log4j.warn(e.getLocalizedMessage() + ", " + e.getCause());

					log4j.warn("Line could not be inserted : " + line);
					System.exit(-1);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Import file " + fileName + " processed .");
		log4j.info("Read " + fileReadCount + " lines from " + fileName + ", inserted " + insertCount + " records ..");
	}

}
