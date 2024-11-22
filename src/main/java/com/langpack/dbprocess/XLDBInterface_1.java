package com.langpack.dbprocess;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.XLUtils;
import com.langpack.datachannel.DataChannel;
import com.langpack.datachannel.DataChannelFactory;

public class XLDBInterface_1 {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("XLDBInterface_1");

	DataChannelFactory factory = new DataChannelFactory();
	DataChannel sourceDatabase = null;
	DataChannel targetDatabase = null;

	Connection dbConn = null;
	String sqlInsert = null;
	PreparedStatement psInsert = null;

	int pingFrequency = 1;
	int flushFrequency = 1;
	int countKeyRead = 0;
	Boolean showEachRead = null;

	LinkedHashMap<String, String> fieldMapping = new LinkedHashMap<>();

	Charset cs1 = Charset.forName("UTF-8");
	ConfigReader cfg = null;

	File importFile = null;

	public XLDBInterface_1(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		DataChannelFactory.initialize(cfgFileName);

		sourceDatabase = DataChannelFactory.getChannelByName("XLFile");
		targetDatabase = DataChannelFactory.getChannelByName("Ledger_Cursor");

		int countField = 0;
		String key = null;
		String value = null;

		do {
			key = "Transfer.TargetFieldMap." + countField;
			value = cfg.getValue("Transfer.TargetFieldMap." + countField);

			if (value != null) {
				fieldMapping.put(key, value);
			}
			countField++;
		} while (value != null);

		sqlInsert = getInsertStatement();
		dbConn = targetDatabase.getDbConn();
		try {
			dbConn.prepareStatement(sqlInsert);
			psInsert = dbConn.prepareStatement(sqlInsert);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void process() {
		Object rowObject = null;

		int skipRows = Integer.parseInt(cfg.getValue("Transfer.SkipLines", "0"));

		for (int i = 0; i <= skipRows; i++) {
			try {
				rowObject = sourceDatabase.getNextRow();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		int rowNumber = skipRows;

		String colStr = null;
		int col = 0;
		try {
			do {
				XSSFRow row = (XSSFRow) rowObject;

				int colCount = 1;
				ArrayList<String> data = XLUtils.readRow(row, 2);
				log4j.info(String.format("Processing row [%s] : %s", rowNumber,
						GlobalUtils.convertArrayToString(data, "; ")));

				for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					if ("".equals(value) || value == null) {
						// skip empty mappings
					} else {
						colStr = key.replace("Transfer.TargetFieldMap.", "");
						col = new Integer(colStr);
						String tmp = data.get(col).trim();
						psInsert.setString(colCount, tmp);
						// log4j.info(String.format("%s %s", colCount, data.get(col)));
						colCount++;
					}

				}
				try {
					psInsert.executeUpdate();
				} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
					log4j.warn(String.format("Line [%s] content [%s] already exists, skipping ..", rowNumber + 1,
							GlobalUtils.convertArrayToString(data)));
				}

				// log4j.info(GlobalUtils.convertArrayToString(data, "; "));
				rowNumber++;
				// System.exit(-1);;
			} while ((rowObject = sourceDatabase.getNextRow()) != null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String getInsertStatement() {
		String retval = "INSERT INTO " + cfg.getValue("Transfer.TargetTable") + " (";
		String fieldsString = "";
		String valuesString = "";

		for (String key : fieldMapping.keySet()) {
			String value = fieldMapping.get(key);
			if ("".equals(value) || value == null) {
				// skip empty mappings
			} else {
				fieldsString = fieldsString.concat(value + ", ");
				// valuesString = valuesString.concat(key + ", ");
				valuesString = valuesString.concat("?, ");
			}
		}
		fieldsString = fieldsString.substring(0, fieldsString.length() - 2);
		valuesString = valuesString.substring(0, valuesString.length() - 2);

		retval = retval + fieldsString + ") VALUES (" + valuesString + ")";
		return retval;
	}

	public static void main(String[] args) {
		log4j.info("Starting country data import ..");

		// TODO Auto-generated method stub
		XLDBInterface_1 instance = new XLDBInterface_1(args[0]);
		instance.process();

		log4j.info("Completed export process");
	}
}
