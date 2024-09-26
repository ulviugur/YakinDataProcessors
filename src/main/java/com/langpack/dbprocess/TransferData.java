package com.langpack.dbprocess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;
import com.langpack.datachannel.DataInterface;

public class TransferData {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("TransferData");

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

	public TransferData(String tmpConfigFile) {

		cfg = new ConfigReader(tmpConfigFile);

		dbDriver1 = cfg.getValue("db1.Driver");
		dbURL1 = cfg.getValue("db1.URL");
		dbUser1 = cfg.getValue("db1.User");
		dbPass1 = cfg.getValue("db1.Password");

		dbDriver2 = cfg.getValue("db2.Driver");
		dbURL2 = cfg.getValue("db2.URL");
		dbUser2 = cfg.getValue("db2.User");
		dbPass2 = cfg.getValue("db2.Password");

		sourceDatabase = new DataInterface("source");
		sourceDatabase.connectDB(dbDriver1, dbURL1, dbUser1, dbPass1);

		targetDatabase = new DataInterface("target");
		targetDatabase.connectDB(dbDriver2, dbURL2, dbUser2, dbPass2);

		dbConn1 = sourceDatabase.getDBConnection();
		dbConn2 = targetDatabase.getDBConnection();
	}

	public void process() {
		Statement stSelect = null;
		PreparedStatement psInsert = null;
		ResultSet rs = null;
		try {
			stSelect = dbConn1.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			psInsert = dbConn2.prepareStatement(cfg.getValue("db2.Insert"));
			stSelect.setFetchSize(Integer.MIN_VALUE);
			log4j.info("starting data query ..");
			rs = stSelect.executeQuery(cfg.getValue("db1.Select"));
			log4j.info("query ended OK, loading data content ..");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long countRead = 0;
		long countInsert = 0;

		boolean moveNext = true;
		while (moveNext) {
			try {
				moveNext = rs.next();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (moveNext) {
				try {
					String id = rs.getString(1);
					String streetName = rs.getString(2);
					String streetType = rs.getString(3);
					String localityName = rs.getString(4);
					String postalCode = rs.getString(5);
					String hno = rs.getString(6);
					String newPostalCode = rs.getString(7);
					String source = rs.getString(8);
					String time = rs.getString(9);
					countRead++;

					log4j.info(String.format("Read (%s) : %s, %s, %s, %s, %s, %s, %s, %s, %s, ", countRead, id,
							streetName, streetType, localityName, postalCode, hno, newPostalCode, source, time));

					psInsert.setString(1, id);
					psInsert.setString(2, streetName);
					psInsert.setString(3, streetType);
					psInsert.setString(4, localityName);
					psInsert.setString(5, postalCode);
					psInsert.setString(6, hno);
					psInsert.setString(7, newPostalCode);
					psInsert.setString(8, source);
					psInsert.setString(9, time);
					psInsert.executeUpdate();
					countInsert++;

					log4j.info(String.format(" record inserted .."));
				} catch (java.sql.SQLIntegrityConstraintViolationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		log4j.info("process completed ..");
	}

}
