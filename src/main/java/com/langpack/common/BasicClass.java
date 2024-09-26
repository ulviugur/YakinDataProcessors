package com.langpack.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BasicClass {

	protected Logger log4j = LogManager.getLogger("BasicClass");

	protected static String dbDriver = null;
	protected static String dbURL = null;
	protected static String dbUser = null;
	protected static String dbPass = null;

	protected String selectString1 = null;
	protected String selectString2 = null;
	protected String selectString3 = null;

	protected String insertString1 = null;
	protected String insertString2 = null;
	protected String insertString3 = null;
	protected String insertString4 = null;
	protected String insertString5 = null;

	protected String updateString1 = null;
	protected String updateString2 = null;
	protected String updateString3 = null;

	protected String deleteString1 = null;
	protected String deleteString2 = null;

	protected Connection DBconn = null;

	protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected PreparedStatement psSelect1 = null;
	protected PreparedStatement psSelect2 = null;
	protected PreparedStatement psSelect3 = null;

	protected PreparedStatement psInsert1 = null;
	protected PreparedStatement psInsert2 = null;
	protected PreparedStatement psInsert3 = null;
	protected PreparedStatement psInsert4 = null;

	protected PreparedStatement psUpdate1 = null;
	protected PreparedStatement psUpdate2 = null;
	protected PreparedStatement psUpdate3 = null;

	protected PreparedStatement psDelete1 = null;
	protected PreparedStatement psDelete2 = null;

	protected ConfigReader cfg = null;

	public BasicClass(String cfgFileName) {

		cfg = new ConfigReader(cfgFileName);

		log4j = LogManager.getLogger(cfgFileName);

		dbDriver = cfg.getValue("db1.Driver");
		dbURL = cfg.getValue("db1.URL");
		dbUser = cfg.getValue("db1.User");
		dbPass = cfg.getValue("db1.Password");

		selectString1 = cfg.getValue("db1.SelectQuery1");
		selectString2 = cfg.getValue("db1.SelectQuery2");
		selectString3 = cfg.getValue("db1.SelectQuery3");

		insertString1 = cfg.getValue("db1.InsertQuery1");
		insertString2 = cfg.getValue("db1.InsertQuery2");
		insertString3 = cfg.getValue("db1.InsertQuery3");
		insertString4 = cfg.getValue("db1.InsertQuery4");

		updateString1 = cfg.getValue("db1.UpdateQuery1");
		updateString2 = cfg.getValue("db1.UpdateQuery2");
		updateString3 = cfg.getValue("db1.UpdateQuery3");

		deleteString1 = cfg.getValue("db1.DeleteQuery1");
		deleteString2 = cfg.getValue("db1.DeleteQuery2");

		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			log4j.info("Establishing connection with the Database ..");
			DBconn = DriverManager.getConnection(dbURL, dbUser, dbPass);
			DBconn.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			psInsert1 = DBconn.prepareStatement(insertString1);
		} catch (SQLException ex) {
			log4j.warn("db1.insertString1 is not specified, skipping !");
		}
		try {
			psInsert2 = DBconn.prepareStatement(insertString2);
		} catch (SQLException ex) {
			log4j.warn("db1.insertString2 is not specified, skipping !");
		}
		try {
			psInsert3 = DBconn.prepareStatement(insertString3);
		} catch (SQLException ex) {
			log4j.warn("db1.insertString3 is not specified, skipping !");
		}
		try {
			psInsert4 = DBconn.prepareStatement(insertString4);
		} catch (SQLException ex) {
			log4j.warn("db1.insertString4 is not specified, skipping !");
		}

		try {
			psSelect1 = DBconn.prepareStatement(selectString1);
		} catch (SQLException ex) {
			log4j.warn("db1.SelectQuery1 is not specified, skipping !");
		}
		try {
			psSelect2 = DBconn.prepareStatement(selectString2);
		} catch (SQLException ex) {
			log4j.warn("db1.SelectQuery2 is not specified, skipping !");
		}
		try {
			psSelect3 = DBconn.prepareStatement(selectString3);
		} catch (SQLException ex) {
			log4j.warn("db1.SelectQuery3 is not specified, skipping !");
		}

		try {
			psUpdate1 = DBconn.prepareStatement(updateString1);
		} catch (SQLException ex) {
			log4j.warn("db1.UpdateString1 is not specified, skipping !");
		}
		try {
			psUpdate2 = DBconn.prepareStatement(updateString2);
		} catch (SQLException ex) {
			log4j.warn("db1.UpdateString2 is not specified, skipping !");
		}
		try {
			psUpdate3 = DBconn.prepareStatement(updateString3);
		} catch (SQLException ex) {
			log4j.warn("db1.UpdateString3 is not specified, skipping !");
		}

		try {
			psDelete1 = DBconn.prepareStatement(deleteString1);
		} catch (SQLException ex) {
			log4j.warn("db1.DeleteString1 is not specified, skipping !");
		}
		try {
			psDelete2 = DBconn.prepareStatement(deleteString2);
		} catch (SQLException ex) {
			log4j.warn("db1.DeleteString2 is not specified, skipping !");
		}

		log4j.info("DB connections established ..");

	}

	public void startProcess() { // uses no arguments as input, it should use the config set for params. Let's
									// try if this way works
		String runMethodName = cfg.getValue("runMethod");
		if (runMethodName == null) {
			log4j.error(String.format("runMethod name is not defined in %s", cfg.getCfgFileName()));
			log4j.error("Exitting !!");
			System.exit(-1);
		}

		Class<?> instanceClass = null;
		Method method = null;

		try {
			Class[] argTypes = ReflectionUtils.getParameterClassArray(new String[] {});
			instanceClass = Class.forName(this.getClass().getName());
			method = instanceClass.getMethod(runMethodName, argTypes);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			log4j.info("***** Invoking : " + instanceClass.getName() + "." + method.getName()
					+ method.getParameterTypes());
			method.invoke(this, new Object[] {});

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setAutoCommit(boolean tmp) {
		try {
			DBconn.setAutoCommit(tmp);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
