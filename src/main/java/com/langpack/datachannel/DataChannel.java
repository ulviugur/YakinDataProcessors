package com.langpack.datachannel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;

public abstract class DataChannel {
	static final String CONFIG_FIELD_NAME = "Name";
	static final String CONFIG_FIELD_URL = "URL";
	static final String CONFIG_FIELD_DRIVER = "Driver";
	static final String CONFIG_FIELD_USER = "User";
	static final String CONFIG_FIELD_PASSWORD = "Password";
	static final String CONFIG_FIELD_IMPORTSHEET = "ImportSheet";
	static final String CONFIG_FIELD_SELECTQUERY = "SelectQuery";
	static final String CONFIG_FIELD_PRELOADDATA = "PreloadData";
	static final String CONFIG_FIELD_UPDATEQUERY = "UpdateQuery";
	static final String CONFIG_FIELD_INSERTQUERY = "InsertQuery";
	static final String CONFIG_FIELD_IMPORTCOLUMNCOUNT = "ImportColumnCount";

	private String channelType = null;
	private String url = null;
	private String driver = null;
	private String user = null;
	private String password = null;
	private String name = null;
	private Boolean preloadData = null;
	private String prefix = null;
	private String importSheetName = null;
	private String importColumnCount = null;
	private Connection dbConn = null;

	String selectQuery = null;
	String insertQuery = null;
	String updateQuery = null;

	ConfigReader cfg = null;

	// Use this class to transfer data from one database to another database
	public Logger log4j = LogManager.getLogger("DataChannel");

	public DataChannel(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);
	}

	public int getImportColumnCount() {
		int retval = Integer.parseInt(importColumnCount);
		return retval;
	}

	public void setImportColumnCount(String importColumnCount) {
		this.importColumnCount = importColumnCount;
	}

	public String getImportSheetName() {
		return importSheetName;
	}

	public void setImportSheetName(String importSheetName) {
		this.importSheetName = importSheetName;
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public DataChannel(ConfigReader cfg, String tmpType, String tmpPrefix) throws UnknownDataChannelException {
		this.cfg = cfg;
		if (tmpType != null) {
			this.setChannelType(tmpType);
		} else {
			UnknownDataChannelException ex = new UnknownDataChannelException(
					"Channel type is not set for the channel !");
			throw ex;
		}
		if (tmpPrefix != null) {
			this.setPrefix(tmpPrefix);
		} else {
			UnknownDataChannelException ex = new UnknownDataChannelException(
					"Config prefix is not set for the channel !");
			throw ex;
		}
		this.setName(cfg.getValue(tmpPrefix + ".Name"));

	}

	public abstract boolean initialize() throws DataChannelParameterMissingException;

	public abstract void closeChannel();

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getPreloadData() {
		return preloadData;
	}

	public void setPreloadData(Boolean preloadData) {
		this.preloadData = preloadData;
	}

	public void setSelectQuery(String selectQuery) {
		this.selectQuery = selectQuery;
	}

	public void setInsertQuery(String insertQuery) {
		this.insertQuery = insertQuery;
	}

	public void setUpdateQuery(String updateQuery) {
		this.updateQuery = updateQuery;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public abstract String getDetails();

	// String representation of DataChannel
	@Override
	public abstract String toString();

	// next record of the LoadQuery
	public abstract Object getNextRow() throws SQLException;

	public abstract Connection getDbConn();

	public abstract void setDbConn(Connection dbConn);

	/*
	 * // prepare the select statement public PreparedStatement getSelectStatement()
	 * throws SQLException { PreparedStatement retval =
	 * getDBconn().prepareStatement(selectQuery); return retval; }
	 *
	 * // prepare the insert statement public PreparedStatement getInsertStatement()
	 * throws SQLException { PreparedStatement retval =
	 * getDBconn().prepareStatement(insertQuery); return retval; }
	 *
	 * // prepare the update statement public PreparedStatement getUpdateStatement()
	 * throws SQLException { PreparedStatement retval =
	 * getDBconn().prepareStatement(updateQuery); return retval; }
	 *
	 * // load data content; can be loaded during initialization or after some time.
	 * Data content is defined with the sql parameter public abstract boolean
	 * loadSqlData(String sql) throws SQLException;
	 *
	 * public String getSelectQuery() { return selectQuery; } public String
	 * getInsertQuery() { return insertQuery; } public String getUpdateQuery() {
	 * return updateQuery; }
	 */

	private String rowToString(HashMap<String, String> row) {
		StringBuilder sb = new StringBuilder("{");
		sb.append(row.get("STREET_BASE_NAME") + ",");
		sb.append(row.get("LOCALITY_NAME") + ",");
		sb.append(row.get("DEPARTMENT_NAME") + ",");
		sb.append(row.get("LEFT_HNO_START") + ",");
		sb.append(row.get("LEFT_HNO_END") + ",");
		sb.append(row.get("RIGHT_HNO_START") + ",");
		sb.append(row.get("RIGHT_HNO_END"));

		/*
		 * Iterator <String> iter = row.keySet().iterator(); while (iter.hasNext()) {
		 * String fieldName = iter.next(); String value = row.get(fieldName);
		 * sb.append(value + ","); }
		 */
		sb.append("}");
		return sb.toString();
	}
}