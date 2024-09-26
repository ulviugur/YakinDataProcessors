package com.langpack.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.hibernate.cfg.Configuration;

public class DBConfigWrapper {

	Configuration cfg = new Configuration();
	Properties prop = new Properties();

	public DBConfigWrapper(String configFile) {
		System.out.println("CfgFile : " + configFile);
		loadHibernateFile(configFile);
	}

	public void loadHibernateFile(String filePath) {

		File hibernateFile = new File(filePath);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(hibernateFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			prop.load(fis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setDialect(prop.getProperty("hibernate.dialect"));
		setDriver_class(prop.getProperty("hibernate.connection.driver_class"));
		setCharacterEncoding(prop.getProperty("hibernate.characterEncoding"));
		setUrl(prop.getProperty("hibernate.connection.url"));
		setUsername(prop.getProperty("hibernate.connection.username"));
		setPassword(prop.getProperty("hibernate.connection.password"));
		// cfg.setProperty("hibernate.jdbc.batch_size",
		// prop.getProperty("hibernate.jdbc.batch_size"));
		// cfg.setProperty("hibernate.jdbc.batch_versioned_data",
		// prop.getProperty("hibernate.jdbc.batch_versioned_data"));

		String strClasses = prop.getProperty("hibernate.classes").replaceAll(" ", "");
		String[] classesArray = strClasses.split(",");
		for (String strClass : classesArray) {
			Class<?> tmpClass = null;
			try {
				tmpClass = Class.forName(strClass);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cfg.addAnnotatedClass(tmpClass);
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getDriver_class() {
		return driver_class;
	}

	public void setDriver_class(String driver_class) {
		this.driver_class = driver_class;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	@Override
	public String toString() {
		return "JDBCConfig [username=" + username + ", password=" + password + ", dialect=" + dialect
				+ ", driver_class=" + driver_class + ", url=" + url + ", characterEncoding=" + characterEncoding + "]";
	}

	String username = "master";
	String password = "Reuters60$";
	// String dialect = "org.hibernate.dialect.Oracle10gDialect";
	String dialect = "org.hibernate.dialect.MySQLDialect";
	// String driver_class = "oracle.jdbc.driver.OracleDriver";
	String driver_class = "com.mysql.jdbc.Driver";
	// String url =
	// "jdbc:oracle:thin:@master.cydjldzxrpz0.us-west-2.rds.amazonaws.com:1521:ORCL";
	String url = "jdbc:mysql://awsmaster.cxappnohxpia.eu-central-1.rds.amazonaws.com:3306:INT";
	String characterEncoding = "UTF-8";
}
