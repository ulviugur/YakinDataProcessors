package com.langpack.integration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.common.TextFileReader;
import com.langpack.scraper.PageParser;

// Read diff records from the database, scrape them and insert into another table
public class OttomanDictImport extends PDFTextStripper {
	public static final Logger logger = LogManager.getLogger("OttomanDictImport");
	private String errorPageIndicator = null;
	private String baseURL = null;
	private String exportFolder = null;

	private PageParser parser = new PageParser();

	static String dbDriver = null;
	static String dbURL = null;
	static String dbUser = null;
	static String dbPass = null;

	String insertString1 = null;
	String selectString1 = null;
	String updateString1 = null;
	String ledgerManagerConfig = null;

	String ruleSkipRestAfterNotFoundHNO = null;
	LedgerManager importManager = null;

	Connection DBconn = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement psSelect = null;
	PreparedStatement psInsert = null;
	PreparedStatement psUpdate = null;

	ConfigReader cfgObject = null;
	String COL_SEPARATOR = "#";

	public OttomanDictImport(String cfgFileName) throws IOException {

		cfgObject = new ConfigReader(cfgFileName);

		HashMap<String, String> map = cfgObject.getParameterMap();
		ledgerManagerConfig = cfgObject.getValue("LedgerManagerConfig");
		importManager = new LedgerManager(ledgerManagerConfig);

		errorPageIndicator = cfgObject.getValue("Download.ErrorPageIndicator");
		baseURL = cfgObject.getValue("Download.Site");
		exportFolder = cfgObject.getValue("Export.Folder");

		dbDriver = cfgObject.getValue("db1.Driver");
		dbURL = cfgObject.getValue("db1.URL");
		dbUser = cfgObject.getValue("db1.User");
		dbPass = cfgObject.getValue("db1.Password");

		selectString1 = cfgObject.getValue("db1.SelectQuery");
		insertString1 = cfgObject.getValue("db1.InsertQuery");
		updateString1 = cfgObject.getValue("db1.UpdateQuery");

		ruleSkipRestAfterNotFoundHNO = cfgObject.getValue("Download.Rules.SkipRestAfterNotFoundHNO", "N");

		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			logger.info("Establishing connection with the Database ..");
			DBconn = DriverManager.getConnection(dbURL, dbUser, dbPass);
			DBconn.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			psInsert = DBconn.prepareStatement(insertString1);
			psSelect = DBconn.prepareStatement(selectString1);
			psUpdate = DBconn.prepareStatement(updateString1);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		logger.info("DB connections established ..");
	}

	public void processPDFDict() {
		String processDirectoryName = "S:\\Ulvi\\wordspace\\Wordlet\\dict";
		// String processDirectoryName = "S:\\Ulvi\\Personal\\SCAN\\TAPU";

		File processDirectory = new File(processDirectoryName);
		if (processDirectory.isDirectory()) {

			File[] files = processDirectory.listFiles();
			for (File file : files) {
				String fileName = file.getAbsolutePath();
				String extension = fileName.substring(fileName.length() - 3, fileName.length());
				String fileRootName = file.getName().replace(extension, "");
				if (file.isDirectory()) {
					// skipping directories
				} else if (!extension.toUpperCase().equals("PDF")) {
					// skipping other files
				} else {
					String tabularFileName = processDirectory.getAbsolutePath() + File.separator + fileRootName + "txt";
					File tabularFile = new File(tabularFileName);
					// writeTabularTextFile(file, tabularFile);
					processTabularTextFile(tabularFile);
				}

			}
		} else {
			logger.info(
					String.format("Given parameter [%s] is not a directory ..", processDirectory.getAbsolutePath()));
			logger.info("Exitting ..");
			System.exit(-1);
		}
	}

	public File writeTabularTextFile(File pdfFile, File tabularFile) {
		PDDocument document = null;

		try {
			document = PDDocument.load(pdfFile);
			setSortByPosition(true);

			BufferedWriter bWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(tabularFile), StandardCharsets.UTF_8));
			// the first row is the header column names
			String[] headers = { "xDirAdj", "yDirAdj", "fontSize", "xScale", "height", "widthOfSpace", "widthDirAdj",
					"unicode" };
			// use pipe as a delimiter (just as a personal preference)
			bWriter.write(String.join(";", headers));
			bWriter.write(System.lineSeparator());
			// this will call #writeString() below with the line text and positions of each
			// char
			writeText(document, bWriter);
			bWriter.close();

		} catch (InvalidPasswordException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tabularFile;
	}

	public void processTabularTextFile(File tabularFile) {
		int COL_XCOORD = 0;
		int COL_YCOORD = 1;
		int COL_FONTSIZE = 2;
		int COL_XSCALE = 3;
		int COL_HEIGHT = 4;
		int COL_WIDTHOFSPACE = 5;
		int COL_WIDTHDIRADJ = 6;
		int COL_TEXT = 7;
		boolean DB_INSERT = true;

		TextFileReader reader = new TextFileReader(tabularFile);
		boolean opened = reader.openFile();

		FileExporter exporter = null;
		try {
			exporter = new FileExporter(tabularFile.getAbsolutePath() + ".2");

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String line = null;
		StringBuffer item = new StringBuffer();
		StringBuffer desc = new StringBuffer();
		int SKIP_LINES = 170;
		if (opened) {
			try {
				boolean descField = false;
				for (int i = 0; i < SKIP_LINES; i++) {
					line = reader.readLine();
				}
				int count = 1;
				while ((line = reader.readLine()) != null) {
					// logger.info(count);
					String[] columns = line.split(COL_SEPARATOR);

					if (columns.length < 2) {
						// empty line, skip
						logger.info(String.format("[%s] Skipping empty line", count));
					} else {
						double xcoord = Double.parseDouble(columns[COL_XCOORD]);
						double ycoord = Double.parseDouble(columns[COL_YCOORD]);
						String text = columns[COL_TEXT];

						if (xcoord < 212.5) {
							if (descField) {
								// satir basi
								descField = false;

								String itemStr = item.toString().trim();
								String descStr = desc.toString().trim();
								descStr = descStr.replace("Tarihvemedeniyet.org", "");
								if (!"".equals(descStr)) {
									String lastChar = null;
									try {
										lastChar = descStr.substring(descStr.length() - 1, descStr.length());
									} catch (java.lang.StringIndexOutOfBoundsException ex) {
										ex.printStackTrace();
									}

									if (",".equals(lastChar)) {
										descStr = descStr.substring(0, descStr.length() - 1);
									}

									if (DB_INSERT) {
										java.util.Date time = new java.util.Date();
										try {
											psInsert.setString(1, itemStr);
											psInsert.setString(2, descStr);
											psInsert.setString(3, format.format(time));

											psInsert.executeUpdate();
										} catch (SQLException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

									} else {
										exporter.writeLineToFile(itemStr + "#" + descStr);
									}
									desc = new StringBuffer();
									item = new StringBuffer();
								}
							}
							item.append(text);
						} else {
							if (!descField) {
								// description'a gectik
								desc = new StringBuffer(text);
								descField = true;
								// logger.info(String.format("[%s] Item => %s", count, itemStr));
							} else {
								desc.append(text);
							}
						}
					}

					// logger.info(String.format("[%s] Line => %s", count, line));
					count++;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void writeWordSeparator() throws IOException {
		// do nothing as we don't need spaces
	}

	@Override
	protected void writeLineSeparator() throws IOException {
		// do nothing as writeString(String, List<TextPosition>) below handles the new
		// lines
	}

	@Override
	protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
		if (!text.isEmpty()) {
			textPositions.forEach(textPosition -> {
				try {
					// call the parent's writeString(String) to write to the output writer
					writeString(String.join(COL_SEPARATOR, String.valueOf(textPosition.getXDirAdj()),
							String.valueOf(textPosition.getYDirAdj()), String.valueOf(textPosition.getFontSize()),
							String.valueOf(textPosition.getXScale()), String.valueOf(textPosition.getHeight()),
							String.valueOf(textPosition.getWidthOfSpace()),
							String.valueOf(textPosition.getWidthDirAdj()), textPosition.getUnicode()));
					// write a line/row after each character
					writeString(System.lineSeparator());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		OttomanDictImport instance = null;

		try {
			instance = new OttomanDictImport(args[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		instance.processPDFDict();
	}
}
