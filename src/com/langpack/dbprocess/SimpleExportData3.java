package com.langpack.dbprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.BasicClass;
import com.langpack.common.ConfigReader;
import com.langpack.datachannel.DataInterface;

public class SimpleExportData3 extends BasicClass {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("SimpleExportData3");

	DataInterface sourceDatabase = null;

	String exportDirectoryName = null;
	String exportFileRootName = null;
	String exportFileExtension = null;
	File exportFile = null;
	String sqlSelect = null;
	String strSegmentSeperator = null;
	String fieldDelimiter = null;

	OutputStreamWriter fileWriter = null;
	BufferedWriter writer = null;

	String[] cfgHeaderFieldNames = null;
	ArrayList<String> dbHeaderFieldNames = null;
	Boolean overwriteExportFile = null;
	Boolean showEachRead = null;
	Integer maximumFiles = null;
	int pingFrequency = 1;
	int flushFrequency = 1;
	int flushCounter = 1;
	int countKeyRead = 0;

	Charset cs1 = Charset.forName("UTF-8");
	DecimalFormat coordsFormatter = new DecimalFormat("#.000000");
	ArrayList<String> row = null;

	ConfigReader cfg = null;

	public SimpleExportData3(String cfgFileName) {
		super(cfgFileName);

		cfg = new ConfigReader(cfgFileName);

		exportDirectoryName = cfg.getValue("Export.Directory");
		exportFileRootName = cfg.getValue("Export.FileRootName");
		exportFileExtension = cfg.getValue("Export.FileExtension", "txt");

		fieldDelimiter = cfg.getValue("Export.Delimiter", ";");

		String tmpHeaderList = cfg.getValue("Export.Headers");
		if (cfg.getValue("Export.HeaderNames.Source").equals("CFG")) {
			cfgHeaderFieldNames = tmpHeaderList.split(fieldDelimiter);
		}

		// if it is not explicitly stated, do not overwrite the export file
		if ("Y".equals(cfg.getValue("Export.FileOverwrite"))) {
			overwriteExportFile = Boolean.TRUE;
		} else {
			overwriteExportFile = Boolean.FALSE;
		}

		// show each row content in the console output
		if ("Y".equals(cfg.getValue("db1.ShowEachRead"))) {
			showEachRead = Boolean.TRUE;
		} else {
			showEachRead = Boolean.FALSE;
		}

		pingFrequency = new Integer(cfg.getValue("db1.PingReadCount", "1")).intValue();

		maximumFiles = new Integer(cfg.getValue("Export.MaximumFiles", "1"));
		flushFrequency = new Integer(cfg.getValue("Export.FlushFrequency", "1")).intValue();
		flushCounter = 0;

		pingFrequency = new Integer(cfg.getValue("db1.PingReadCount", "1")).intValue();

		sourceDatabase = new DataInterface("source");

		if (!openExportFile()) {
			log4j.fatal("Export file could not be opened successfully, cannot proceed ..");
			log4j.info("Exitting !");
			System.exit(-1);
		}
	}

	public void process() {
		int queryID = 1;
		do {
			sqlSelect = cfg.getValue("db1.Select" + queryID);
			log4j.info(sqlSelect);
			/*
			 * Statement stSelect = null; ResultSet rsSelect = null; try { stSelect =
			 * DBconn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
			 * ResultSet.CONCUR_READ_ONLY); //stSelect.setFetchSize(Integer.MIN_VALUE);
			 * log4j.info("Starting Select query .."); rsSelect =
			 * stSelect.executeQuery(sqlSelect);
			 * log4j.info("Query ended OK, loading data content .."); } catch (SQLException
			 * e) { // TODO Auto-generated catch block e.printStackTrace(); }
			 *
			 * ResultSetMetaData meta = null; try { meta = rsSelect.getMetaData();
			 * dbHeaderFieldNames = new ArrayList<String>(); for (int i = 1; i <=
			 * meta.getColumnCount(); i++) { dbHeaderFieldNames.add(meta.getColumnName(i));
			 * } } catch (SQLException e1) { // TODO Auto-generated catch block
			 * e1.printStackTrace(); } log4j.info("Writing headers to the export file");
			 * writeHeaderFields();
			 *
			 * ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>(); try {
			 * while (rsSelect.next()) { countKeyRead++; ArrayList<String> rowData = new
			 * ArrayList<String>(); for (int i = 1; i <= meta.getColumnCount(); i++) {
			 * rowData.add(rsSelect.getString(i)); } data.add(rowData);
			 *
			 * if (showEachRead.booleanValue()) { log4j.info(String.format("Read row : %s",
			 * rowToString(rowData))); } else if (countKeyRead%pingFrequency == 0) {
			 * log4j.info(String.format("Reached %s records read ..", countKeyRead)); } } if
			 * ("Y".equals(cfg.getValue("Export.WriteRawData", "Y"))) { exportData(data); }
			 * } catch (SQLException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 *
			 * closeExportFile();
			 */
			queryID++;
		} while ((sqlSelect = cfg.getValue("db1.Select" + queryID)) != null);
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

	private void exportData(ArrayList<ArrayList<String>> tmpData) {
		int fieldCount = tmpData.get(0).size();
		Method exportMethod = null;
		try {
			exportMethod = this.getClass().getMethod(cfg.getValue("Export.Method"), null);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (ArrayList<String> element : tmpData) {
			row = element;
			String writeLine = null;
			try {
				writeLine = (String) exportMethod.invoke(this);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writeLinetoExportFile(writeLine);
		}

	}

	private void writeHeaderFields() {
		// Write headers as first row
		String headerSource = cfg.getValue("Export.HeaderNames.Source", "DB");
		Object[] headerArray = null;
		if ("DB".equals(headerSource)) {
			headerArray = dbHeaderFieldNames.toArray();
		} else {
			headerArray = cfgHeaderFieldNames;
		}
		if (headerArray != null) {
			for (int i = 0; i < headerArray.length; i++) {
				if (i != headerArray.length - 1) {
					writetoExportFile(headerArray[i] + fieldDelimiter);
				} else {
					writetoExportFile(headerArray[i] + "\n");
				}
			}
		}
	}

	private File getExportFile() {
		File retval = null;
		File exportDirectory = new File(exportDirectoryName);
		if (!exportDirectory.exists()) {
			log4j.fatal(String.format("Export directory '%s' does not exist, cannot create export file !!!",
					exportDirectory.getAbsolutePath()));
			return null;
		} else if (!exportDirectory.canWrite()) {
			log4j.fatal(String.format("Cannot write into directory '%s', cannot create export file !!!"));
			return null;
		}
		java.util.Date currTime = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		String strDate = formatter.format(currTime);
		if (overwriteExportFile) {
			exportFileRootName = exportDirectoryName + "\\" + exportFileRootName + "_" + strDate + "_1."
					+ exportFileExtension;
			retval = new File(exportFileRootName);
		} else {
			int count = 1;
			String baseExportFileName = exportDirectoryName + "\\" + exportFileRootName + "_" + strDate;
			while (true) {
				String exportFileName = baseExportFileName + "_" + count + "." + exportFileExtension;
				retval = new File(exportFileName);
				if (!retval.exists()) {
					break;
				} else {
					if (count < maximumFiles.intValue()) {
						count++;
					} else {
						log4j.fatal(String.format(
								"Reached maximum files limit of %s files, exitting without export to %s !!!",
								maximumFiles.intValue(), retval.getAbsolutePath()));
						log4j.info(String.format("Please delete unused files from %s and try again ..",
								exportFileRootName));
						retval = null;
						break;
					}
				}
			}
		}
		return retval;
	}

	public boolean openExportFile() {
		boolean retval = false;
		log4j.info("Opening export file ..");
		exportFile = getExportFile();
		if (exportFile == null) {
			log4j.fatal("Export file cound not be identified !");
			log4j.info("Exitting !");
			System.exit(-1);
		} else {
			fileWriter = null;
			try {
				fileWriter = new OutputStreamWriter(new FileOutputStream(exportFile), cs1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			writer = new BufferedWriter(fileWriter);

			log4j.info(String.format("Opening export file %s succesfully ..", exportFile.getAbsolutePath()));
			retval = true;
		}
		return retval;
	}

	public void writeLinetoExportFile(String row) {
		flushCounter++;
		try {
			writetoExportFile(row);
			if (flushCounter >= flushFrequency) {
				writer.flush();
				fileWriter.flush();
				flushCounter = 0;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writetoExportFile(String row) {
		try {
			writer.write(row);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeExportFile() {
		try {
			writer.flush();
			fileWriter.flush();

			writer.close();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		log4j.info("Starting country data export ..");

		// TODO Auto-generated method stub
		SimpleExportData3 instance = new SimpleExportData3(args[0]);
		instance.process();

		log4j.info("Completed export process");
	}

	public String getExportLine_Ledger() {
		StringBuffer baseLine = new StringBuffer();

		for (String element : row) {
			String value = element;
			if (value == null) {
				value = "";
			}
			baseLine = baseLine.append(value);
			baseLine = baseLine.append(fieldDelimiter);
		}
		baseLine = baseLine.append(fieldDelimiter);
		baseLine.append("\r\n");
		return baseLine.toString().substring(0, baseLine.length() - 1);
	}
}
