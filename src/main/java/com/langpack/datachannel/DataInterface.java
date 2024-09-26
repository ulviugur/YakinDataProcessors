package com.langpack.datachannel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataInterface {
	String name = "Unnamed";
	Connection dbConn = null;

	private String dbDriver = null;
	private String dbURL = null;
	private String dbUser = null;
	private String dbPass = null;

	public static final Logger logger = LogManager.getLogger("DataInterface");

	public DataInterface(String _name) {
		this.name = _name;
	}

	public Connection connectDB(String driverClass, String URL, String userid, String pass) {
		this.dbDriver = driverClass;
		this.dbURL = URL;
		this.dbUser = userid;
		this.dbPass = pass;
		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			logger.info(String.format("Establishing connection with the database : %s", this.dbURL));
			dbConn = DriverManager.getConnection(dbURL, dbUser, dbPass);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dbConn;
	}

	public Connection getDBConnection() {
		return this.dbConn;
	}

	@Override
	public String toString() {
		return String.format("%s >> %s >> %s >> %s ", name, dbDriver, dbURL, dbUser);
	}
}
