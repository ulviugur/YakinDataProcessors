package com.langpack.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCExample {

	private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
	private static final String DB_CONNECTION = "jdbc:oracle:thin:@RZR:1521:RZR";
	private static final String DB_USER = "master";
	private static final String DB_PASSWORD = "master";

	public static void main(String[] argv) {

		try {

			selectRecordsFromDbUserTable();

		} catch (SQLException e) {

			System.out.println(e.getMessage());

		}

	}

	private static void selectRecordsFromDbUserTable() throws SQLException {

		Connection dbConnection = null;
		Statement statement = null;

		String selectTableSQL = "select id, street, street_2, locality, department from france_address";

		try {
			dbConnection = getDBConnection();
			statement = dbConnection.createStatement();

			System.out.println(selectTableSQL);

			// execute select SQL stetement
			ResultSet rs = statement.executeQuery(selectTableSQL);

			int count = 0;
			while (rs.next()) {
				count++;
				String fld1 = rs.getString(1);
				String fld2 = rs.getString(2);
				String fld3 = rs.getString(3);
				String fld4 = rs.getString(4);
				String fld5 = rs.getString(5);
				System.out
						.println(String.format(" <%s> field1 : %s, field2 : %s, field4 : %s, field4 : %s, field5 : %s",
								count, fld1, fld2, fld3, fld4, fld5));
			}

		} catch (SQLException e) {

			System.out.println(e.getMessage());

		} finally {

			if (statement != null) {
				statement.close();
			}

			if (dbConnection != null) {
				dbConnection.close();
			}

		}

	}

	private static Connection getDBConnection() {

		Connection dbConnection = null;

		try {

			Class.forName(DB_DRIVER);

		} catch (ClassNotFoundException e) {

			System.out.println(e.getMessage());

		}

		try {

			dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
			return dbConnection;

		} catch (SQLException e) {

			System.out.println(e.getMessage());

		}

		return dbConnection;

	}

}