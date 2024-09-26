package com.langpack.datachannel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.langpack.common.ConfigReader;

public class MySQLDataChannel extends DataChannel {

	ResultSet loadResultset = null;
	ResultSetMetaData metadataLoad = null;

	ArrayList<HashMap<String, String>> tableMatrix = null;
	int tableMatrixCount = 0;
	private Connection dbConn = null;

	public MySQLDataChannel(ConfigReader cfg, String tmpType, String tmpPrefix) throws UnknownDataChannelException {
		super(cfg, tmpType, tmpPrefix);
	}

	@Override
	public Connection getDbConn() {
		return dbConn;
	}

	@Override
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	public boolean isValid() {
		boolean retval = false;
		if (this.getDbConn() != null) {
			try {
				if (!this.getDbConn().isClosed()) {
					retval = true;
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return retval;
	}

	@Override
	public String getDetails() {
		return this.toString();
	}

	@Override
	public String toString() {
		System.out.println("");
		return String.format("Name=%s : <URL=%s, Driver=%s, User=%s, Password=%s, LoadQuery=%s, UpdateQuery=%s>",
				this.getName(), this.getUrl(), this.getDriver(), this.getUser(), this.getPassword());
	}

	private HashMap<String, String> readNextRow() throws SQLException {
		HashMap<String, String> retval = new HashMap<>();
		if (metadataLoad == null) {
			PreparedStatement psLoad = this.getDbConn().prepareStatement(selectQuery);
			loadResultset = psLoad.executeQuery();
			metadataLoad = loadResultset.getMetaData();
		}
		boolean exists = loadResultset.next();
		if (exists) {
			for (int i = 1; i <= metadataLoad.getColumnCount(); i++) {
				String fieldName = metadataLoad.getColumnLabel(i);
				String value = loadResultset.getString(i);
				retval.put(fieldName, value);
			}
		} else {
			retval = null;
		}
		return retval;
	}

	@Override
	public boolean initialize() throws DataChannelParameterMissingException {
		boolean retval = false;

		this.setUrl(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_URL));
		this.setDriver(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_DRIVER));
		this.setUser(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_USER));
		this.setPassword(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_PASSWORD));
		this.setName(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_NAME));

		this.setSelectQuery(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_SELECTQUERY));
		this.setPreloadData(new Boolean(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_PRELOADDATA)));
		this.setUpdateQuery(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_UPDATEQUERY));
		this.setInsertQuery(cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_INSERTQUERY));

		if (this.getUrl() == null || this.getDriver() == null || this.getUser() == null || this.getPassword() == null) {
			String configStr = String.format("Channel %s [%s] : < url=%s, driver=%s, user=%s, password=%s>",
					this.getName(), this.getPrefix(), this.getUrl(), this.getDriver(), this.getUser(),
					this.getPassword());
			throw new DataChannelParameterMissingException(configStr);
		}

		try {
			Class.forName(this.getDriver());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			dbConn = DriverManager.getConnection(this.getUrl(), this.getUser(), this.getPassword());
			retval = true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	@Override
	public void closeChannel() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getNextRow() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
}
