package com.langpack.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

public class TextFileImporter {

	private Connection dbConn = null;

	private String dbDriver = null;
	private String dbURL = null;
	private String dbUser = null;
	private String dbPass = null;

	private String importFileName = null;
	private File importFile = null;
	private String importQuery = null;
	private int skipLines = 0;

	private String updateFileName = null;
	private File updateFile = null;
	private String selectQuery = null;
	private String updateQuery = null;

	private ArrayList<Integer> importMapArray = new ArrayList<>();
	private ArrayList<Integer> updateMapArray = new ArrayList<>();
	private ArrayList<ArrayList<String>> updateReference = new ArrayList<>();
	private ArrayList<HashMap<Integer, Integer>> keyMap = new ArrayList<>();

	private final Charset cset = Charset.forName("UTF-8");
	private String delimiter = ";";

	public static final Logger log4j = LogManager.getLogger("TextFileImporter");

	public static String HBNT_FILE = null;
	private SessionFactory factory;
	private Session session = null;
	private boolean treatEmptyAsNull = false;
	ConfigReader cfg = null;

	public TextFileImporter(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		HBNT_FILE = cfg.getValue("db1.hibernatefile");
		if (HBNT_FILE != null) {
			initialize(HBNT_FILE);
		} else {
			initialize();
		}

		importFileName = cfg.getValue("db1.InsertFile");
		importQuery = cfg.getValue("db1.InsertQuery");
		skipLines = Integer.parseInt(cfg.getValue("db1.InsertFile.SkipLines", "1"));
		delimiter = cfg.getValue("db1.InsertFile.Delimiter", ";");

		updateFileName = cfg.getValue("db1.UpdateFile");
		updateQuery = cfg.getValue("db1.UpdateQuery");
		selectQuery = cfg.getValue("db1.SelectQuery");
		treatEmptyAsNull = Boolean.getBoolean(cfg.getValue("db1.UpdateQuery.TreatEmptyAsNull", "false"));

		log4j.info("Processing mapping of fields ..");
		int count = 1;
		while (true) {
			String tmpFieldMap = cfg.getValue("db1.InsertQuery.Data.Field" + count);
			if (tmpFieldMap != null) {
				importMapArray.add(Integer.parseInt(tmpFieldMap));
			} else {
				// reached end of field mappings
				break;
			}
			count++;
		}

		// Update mapping

		log4j.info("Processing mapping of key fields to update only specific records ..");
		count = 1;
		while (true) {
			String key = "db1.UpdateQuery.Where.KeyMap" + count;
			String tmpMap = cfg.getValue(key);

			if (tmpMap != null) {
				// database field : text field pairs
				String[] flds = tmpMap.split(":");
				HashMap<Integer, Integer> keys = new HashMap<>();
				Integer id = Integer.valueOf(flds[0]);
				Integer field = Integer.valueOf(flds[1]);
				keys.put(id, field);
				keyMap.add(keys);
			} else {
				// reached end of field mappings
				break;
			}
			count++;
		}

		count = 1;
		while (true) {
			String tmpFieldMap = cfg.getValue("db1.UpdateQuery.Data.Field" + count);
			if (tmpFieldMap != null) {
				updateMapArray.add(Integer.parseInt(tmpFieldMap));
			} else {
				// reached end of field mappings
				break;
			}
			count++;
		}
	}

	public void initialize() {
		dbDriver = cfg.getValue("db1.Driver");
		dbURL = cfg.getValue("db1.URL");
		dbUser = cfg.getValue("db1.User");
		dbPass = cfg.getValue("db1.Password");

		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			log4j.info(String.format("Establishing connection to target DB %s..", dbURL));
			dbConn = DriverManager.getConnection(dbURL, dbUser, dbPass);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("DB connection established ..");

	}

	public void initialize(String tmpFile) {
		try {

			Properties prop = new Properties();
			FileInputStream is = new FileInputStream(tmpFile);
			prop.load(is);

			Configuration cfg = new Configuration();

			cfg.setProperty("hibernate.dialect", prop.getProperty("hibernate.dialect"));
			cfg.setProperty("hibernate.connection.driver_class", prop.getProperty("hibernate.connection.driver_class"));
			cfg.setProperty("hibernate.connection.url", prop.getProperty("hibernate.connection.url"));
			cfg.setProperty("hibernate.connection.username", prop.getProperty("hibernate.connection.username"));
			cfg.setProperty("hibernate.connection.password", prop.getProperty("hibernate.connection.password"));

			String strClasses = prop.getProperty("hibernate.classes").replaceAll(" ", "");
			String[] classesArray = strClasses.split(",");
			for (String strClass : classesArray) {
				Class<?> tmpClass = Class.forName(strClass);
				cfg.addAnnotatedClass(tmpClass);
			}

			factory = cfg.buildSessionFactory();
			session = factory.openSession();

			dbConn = factory.getSessionFactoryOptions().getServiceRegistry().getService(ConnectionProvider.class)
					.getConnection();

		} catch (Throwable ex) {
			System.err.println("Failed to create sessionFactory object." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public void process() {
		log4j.info("Process started ..");
		if (cfg.getValue("db1.Actions") == null) {
			log4j.error("Configuration ERROR : db1.Actions is not defined !!");
		}
		if (cfg.getValue("db1.Actions").contains("INSERT")) {
			importData();
		}
		if (cfg.getValue("db1.Actions").contains("UPDATE")) {
			updateData2();
		}
		factory.close();
	}

	private void importData() {
		int fileReadCount = 0;
		int insertCount = 0;

		importFile = new File(importFileName);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(importFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStreamReader freader = new InputStreamReader(fstream, cset);
		BufferedReader reader = new BufferedReader(freader, 1024);

		String line = null;
		PreparedStatement psImport = null;

		try {
			psImport = dbConn.prepareStatement(importQuery);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<String> fields = null;
		try {
			while ((line = reader.readLine()) != null) {
				fileReadCount++;
				if (fileReadCount > skipLines) {
					log4j.info(String.format("Reading insert line (%s) : %s", fileReadCount, line));
					try {
						fields = splitLine(line);

						int fieldId = 1;
						for (; fieldId < importMapArray.size() + 1; fieldId++) {
							int mapId = fieldId - 1;
							int getId = importMapArray.get(mapId) - 1;
							String value = null;
							try {
								value = fields.get(getId);
								psImport.setString(fieldId, value);
							} catch (Exception ex) {
								psImport.setString(fieldId, value);
							}
							// log4j.info(">>>>> :" + value);

						}
						psImport.executeUpdate();
						insertCount++;

					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						log4j.warn(e.getLocalizedMessage() + ", " + e.getCause());
						log4j.warn("Line could not be updated : " + line);
					}
				}
			}
			log4j.info("Committing updates .. ");
			Statement stCommit;
			try {
				stCommit = dbConn.createStatement();
				stCommit.executeQuery("commit");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log4j.info(".. comitted updates");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Read " + fileReadCount + " lines from " + importFileName + ", inserted " + insertCount
				+ " records ..");
	}

	// Run updates on the list provided by the text file
	private void updateData2() {
		int lineCount = 0;
		int updateCount = 0;

		PreparedStatement psUpdate = null;

		try {
			psUpdate = dbConn.prepareStatement(updateQuery);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		DelimitedFileLoader textLoader = new DelimitedFileLoader(null);
		textLoader.loadFile(updateFileName, 1);
		HashMap<Integer, ArrayList<String>> lines = textLoader.getDataset();

		for (Integer lineId : lines.keySet()) {
			lineCount++;
			ArrayList<String> line = lines.get(lineId);
			Integer dbField = -1;
			Integer textColumn = -1;

			try {
				for (HashMap<Integer, Integer> fieldMap : keyMap) {
					dbField = fieldMap.entrySet().iterator().next().getKey();
					textColumn = fieldMap.entrySet().iterator().next().getValue();
					String newValue = null;
					try {
						newValue = line.get(textColumn - 1);
					} catch (java.lang.IndexOutOfBoundsException e) {
						// e.printStackTrace();
					}

					if (newValue == null) {
						if (treatEmptyAsNull) {
							// leave value as it is
						} else {
							newValue = "";
						}
					}

					// log4j.info(String.format("Putting %s in fieldId %s", newValue, dbField));
					if (newValue != null) {
						newValue = newValue.trim();
					}
					psUpdate.setString(dbField.intValue(), newValue);
					// log4j.info(newValue);
				}

				int res = psUpdate.executeUpdate();
				log4j.info(String.format("{ %s } Updated %s records with %s", lineCount, res,
						GlobalUtils.convertArraytoString(line)));
				updateCount++;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log4j.info("Updated number of datasets : " + updateCount);
	}

	// Run updates on a pre-selected dataset
	private void updateData() {
		// read the file which will enhance the table content
		readReferenceFile();

		int updateCount = 0;

		PreparedStatement psSelect = null;
		PreparedStatement psUpdate = null;

		try {
			psSelect = dbConn.prepareStatement(selectQuery);
			psUpdate = dbConn.prepareStatement(updateQuery);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			// read the respective row records from the database
			ResultSet rs = psSelect.executeQuery();
			while (rs.next()) {
				int dbField = -1;
				int textColumn = -1;

				HashMap<Integer, String> keyValues = new HashMap<>();

				// create a list of the key / values
				for (HashMap<Integer, Integer> tmpMap : keyMap) {
					dbField = tmpMap.keySet().iterator().next();
					textColumn = tmpMap.values().iterator().next();
					String value = rs.getString(dbField);
					keyValues.put(textColumn, value);
				}

				// find the relevant rows from the text file
				ArrayList<ArrayList<String>> rows = findRelevantRows(keyValues);
				if (rows != null && rows.size() > 0) {
					for (ArrayList<String> row : rows) {
						int fieldId = 1;
						for (; fieldId < updateMapArray.size() + 1; fieldId++) {
							// log4j.info(">>>>> :" + myField);
							String newValue = row.get(updateMapArray.get(fieldId - 1));
							psUpdate.setString(fieldId, newValue);
						}
						psUpdate.executeUpdate();
						updateCount++;
					}
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Updated number of records : " + updateCount);
	}

	// find all relevant rows matching the fieldId / value pairs
	private ArrayList<ArrayList<String>> findRelevantRows(HashMap<Integer, String> keyValues) { // <fieldId , expected
																								// value>
		ArrayList<ArrayList<String>> retval = new ArrayList<>();
		for (int i = 0; i < updateReference.size(); i++) {
			ArrayList<String> row = updateReference.get(i);
			boolean valid = true;
			for (Integer id : keyValues.keySet()) {
				String expectedValue = keyValues.get(id);
				String textLineValue = row.get(id - 1); // id starts from one, normalize it to 0
				log4j.info(String.format("Expected value : %s, fileValue = %s", expectedValue, textLineValue));
				if (!textLineValue.equals(expectedValue)) {
					valid = false;
					log4j.info("Nope");
					break;
				} else {
					log4j.info("Yep");
				}
				log4j.info("");
			}
			if (valid) {
				retval.add(row);
			}
		}
		return retval;
	}

	private void readReferenceFile() {
		int fileReadCount = 0;
		String line = null;
		updateFile = new File(updateFileName);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(updateFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStreamReader freader = new InputStreamReader(fstream, cset);
		BufferedReader reader = new BufferedReader(freader, 1024);
		ArrayList<String> fields = null;
		try {
			while ((line = reader.readLine()) != null) {
				fileReadCount++;
				if (fileReadCount > skipLines) {
					fields = splitLine(line);
					updateReference.add(fields);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		log4j.info("Starting country data import ..");

		// TODO Auto-generated method stub
		TextFileImporter instance = new TextFileImporter(args[0]);
		instance.process();

		log4j.info("Completed import process");
	}

	private ArrayList<String> splitLine(String tmpLine) {

		ArrayList<String> retval = new ArrayList<>();
		boolean openQuotes = false;
		boolean skipNext = false; // in case quotes are closed, skip the next comma
		String field = "";
		for (int i = 0; i < tmpLine.length(); i++) {
			String letter = tmpLine.substring(i, i + 1);
			if (letter.equals("\"")) {
				if (openQuotes) {
					retval.add(field);
					field = "";
					openQuotes = false;
					skipNext = true;
				} else {
					openQuotes = true;
				}
			} else if (letter.equals(delimiter) && !openQuotes) {
				if (!skipNext) {
					retval.add(field);
					field = "";
				} else {
					skipNext = false;
				}
			} else {
				field = field + letter;
			}
			// log4j.info(field);
		}
		field = field.trim();
		if (!"".equals(field)) {
			retval.add(field);
		}
		return retval;
	}
}
