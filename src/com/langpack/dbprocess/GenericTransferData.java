package com.langpack.dbprocess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;
import com.langpack.datachannel.DataInterface;

public class GenericTransferData {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("GenericTransferData");

	private Connection dbConn1 = null;

	private String dbDriver1 = null;
	private String dbURL1 = null;
	private String dbUser1 = null;
	private String dbPass1 = null;

	private Connection dbConn2 = null;

	private String dbDriver2 = null;
	private String dbURL2 = null;
	private String dbUser2 = null;
	private String dbPass2 = null;

	DataInterface sourceDatabase = null;
	DataInterface targetDatabase = null;

	ConfigReader cfg = null;

	public GenericTransferData(String tmpConfigFile) {

		cfg = new ConfigReader(tmpConfigFile);

		dbDriver1 = cfg.getValue("db1.Driver");
		dbURL1 = cfg.getValue("db1.URL");
		dbUser1 = cfg.getValue("db1.User");
		dbPass1 = cfg.getValue("db1.Password");

		dbDriver2 = cfg.getValue("db2.Driver");
		dbURL2 = cfg.getValue("db2.URL");
		dbUser2 = cfg.getValue("db2.User");
		dbPass2 = cfg.getValue("db2.Password");

		try {
			dbConn1 = DriverManager.getConnection(dbURL1, dbUser1, dbPass1);
			dbConn1.setAutoCommit(true);
			log4j.info("Connected to DB1");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			dbConn2 = DriverManager.getConnection("jdbc:mysql://ACER1:3306/words", "ulvi", "ulvi");
			dbConn2.setAutoCommit(true);
			log4j.info("Connected to DB2");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void process() {
		log4j.info("Starting transfer of data ..");
		String selectString = String.format("SELECT * FROM %s", cfg.getValue("db1.SourceTable"));

		Statement stSelect = null;
		PreparedStatement psInsert = null;
		ResultSet rs = null;
		try {
			stSelect = dbConn1.createStatement();
			log4j.info("starting data query ..");
			rs = stSelect.executeQuery(selectString);
			log4j.info("query ended OK, loading data content ..");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ResultSetMetaData md = null;
		int colCount = 0;
		try {
			md = rs.getMetaData();
			colCount = md.getColumnCount();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String insertSQLPart1 = "INSERT INTO " + cfg.getValue("db2.TargetTable") + "(";
		String insertSQLPart2 = "(";

		try {
			for (int i = 1; i <= md.getColumnCount(); i++) {
				String tmpName = md.getColumnName(i);
				insertSQLPart1 = insertSQLPart1 + String.format("%s,", tmpName);
				insertSQLPart2 = insertSQLPart2 + "?,";
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		insertSQLPart1 = insertSQLPart1.substring(0, insertSQLPart1.length() - 1) + ")";
		insertSQLPart2 = insertSQLPart2.substring(0, insertSQLPart2.length() - 1) + ")";

		String insertSQL = insertSQLPart1 + " VALUES " + insertSQLPart2;
		try {
			psInsert = dbConn2.prepareStatement(insertSQL);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Object[] dataArray = new Object[colCount];

		long countRead = 0;

		try {
			while (rs.next()) {
				// dataArray = new Object[colCount];
				countRead++;
				for (int i = 1; i <= colCount; i++) {
					Object value = rs.getObject(i);
					psInsert.setObject(i, value);
				}
				psInsert.executeUpdate();

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info("Completed transfer process !");
	}

	public static void main(String[] args) {

		// TODO Auto-generated method stub
		GenericTransferData instance = new GenericTransferData(args[0]);
		instance.process();
	}

}
