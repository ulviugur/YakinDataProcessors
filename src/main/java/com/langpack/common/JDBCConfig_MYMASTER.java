package com.langpack.common;

public class JDBCConfig_MYMASTER {

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

	String username = "ulvi";
	String password = "ulvi";
	// String dialect = "org.hibernate.dialect.Oracle10gDialect";
	String dialect = "org.hibernate.dialect.MySQLDialect";
	// String driver_class = "oracle.jdbc.driver.OracleDriver";
	String driver_class = "com.mysql.jdbc.Driver";
	// String url =
	// "jdbc:oracle:thin:@master.cydjldzxrpz0.us-west-2.rds.amazonaws.com:1521:ORCL";
	String url = "jdbc:mysql://localhost:3305:MYMASTER";
	String characterEncoding = "UTF-8";
}
