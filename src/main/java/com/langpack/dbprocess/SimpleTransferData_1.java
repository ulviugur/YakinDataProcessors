package com.langpack.dbprocess;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;

public class SimpleTransferData_1 {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("SimpleExportData3");

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

	DataChannelFactory factory = new DataChannelFactory();
	DataChannel sourceDatabase = null;
	DataChannel targetDatabase = null;

	File exportFile = null;
	String sqlSelect = null;
	String sqlInsert = null;
	PreparedStatement psInsert = null;
	int pingFrequency = 1;
	int flushFrequency = 1;
	int countKeyRead = 0;
	Boolean showEachRead = null;

	ArrayList<Integer> fieldMapping = new ArrayList<>();

	Charset cs1 = Charset.forName("UTF-8");
	ConfigReader cfg = null;

	public SimpleTransferData_1(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		dbDriver1 = cfg.getValue("db1.Driver");
		dbURL1 = cfg.getValue("db1.URL");
		dbUser1 = cfg.getValue("db1.User");
		dbPass1 = cfg.getValue("db1.Password");
		sqlSelect = cfg.getValue("db1.Select");

		dbDriver2 = cfg.getValue("db2.Driver");
		dbURL2 = cfg.getValue("db2.URL");
		dbUser2 = cfg.getValue("db2.User");
		dbPass2 = cfg.getValue("db2.Password");
		sqlInsert = cfg.getValue("db2.Insert");

		pingFrequency = new Integer(cfg.getValue("db1.PingReadCount", "1")).intValue();
		flushFrequency = new Integer(cfg.getValue("Export.FlushFrequency", "1")).intValue();
		pingFrequency = new Integer(cfg.getValue("db1.PingReadCount", "1")).intValue();

		// show each row content in the console output
		if ("Y".equals(cfg.getValue("db1.ShowEachRead"))) {
			showEachRead = Boolean.TRUE;
		} else {
			showEachRead = Boolean.FALSE;
		}

		sourceDatabase = DataChannelFactory.getChannelByName("source");
		dbConn1 = sourceDatabase.getDbConn();

		targetDatabase = DataChannelFactory.getChannelByName("target");
		dbConn2 = targetDatabase.getDbConn();

		try {
			psInsert = dbConn2.prepareStatement(sqlInsert);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int countField = 0;
		String tmp = "";
		while (tmp != null) {
			tmp = cfg.getValue("Transfer.TargetFieldMap." + countField);
			if (tmp != null) {
				fieldMapping.add(new Integer(tmp));
			}
			countField++;
		}
	}

	public void process() {
		Statement stSelect = null;
		ResultSet rsSelect = null;
		int sourceColumn = -1;
		int targetColumn = -1;
		try {
			stSelect = dbConn1.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			// stSelect.setFetchSize(Integer.MIN_VALUE);
			log4j.info("Starting Select query ..");
			rsSelect = stSelect.executeQuery(sqlSelect);
			log4j.info("Query ended OK, loading data content ..");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (rsSelect.next()) {
				countKeyRead++;
				ArrayList<String> rowData = new ArrayList<>();
				for (int i = 0; i < fieldMapping.size(); i++) {
					targetColumn = i + 1;
					sourceColumn = fieldMapping.get(i) + 1;
					String value = rsSelect.getString(sourceColumn);
					log4j.info("Processing column " + i);
					psInsert.setString(targetColumn, value);
					rowData.add(value);
				}
				try {
					psInsert.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (showEachRead.booleanValue()) {
					log4j.info(String.format("Read row : %s", rowToString(rowData)));
				} else if (countKeyRead % pingFrequency == 0) {
					log4j.info(String.format("Reached %s records read ..", countKeyRead));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String rowToString(ArrayList<String> tmp) {
		String retval = "Record : [";

		for (String element : tmp) {
			String value = element;
			if (value == null) {
				value = "";
			}
			retval = retval.concat(value);
			retval = retval.concat(",");
		}
		retval = retval.concat("]");
		return retval;
	}

	public static void main(String[] args) {

		// TODO Auto-generated method stub
		SimpleTransferData_1 instance = new SimpleTransferData_1(args[0]);
		instance.process();

		log4j.info("Completed transfer process");
	}
}
