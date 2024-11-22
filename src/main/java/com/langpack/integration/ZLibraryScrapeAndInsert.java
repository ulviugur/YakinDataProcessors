package com.langpack.integration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.activation.MimetypesFileTypeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.ReflectionUtils;

// Read diff records from the database, scrape them and insert into another table
public class ZLibraryScrapeAndInsert {
	public static final Logger logger = LogManager.getLogger("ZLibraryScrapeAndInsert");
	private String baseURL = null;

	static String dbDriver = null;
	static String dbURL = null;
	static String dbUser = null;
	static String dbPass = null;

	String insertString1 = null;
	String selectString1 = null;
	String updateString1 = null;
	String ledgerManagerConfig = null;
	String processDirectoryName = null;

	String ruleSkipRestAfterNotFoundHNO = null;
	LedgerManager ledgerManager = null;

	Connection DBconn = null;

	TreeSet<String> postalCodeList = new TreeSet<>();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	PreparedStatement psSelect = null;
	PreparedStatement psInsert = null;
	PreparedStatement psUpdate = null;

	ConfigReader cfgObject = null;

	public ZLibraryScrapeAndInsert(String cfgFileName) {

		cfgObject = new ConfigReader(cfgFileName);

		HashMap<String, String> map = cfgObject.getParameterMap();
		ledgerManagerConfig = cfgObject.getValue("LedgerManagerConfig");
		ledgerManager = new LedgerManager(ledgerManagerConfig);

		baseURL = cfgObject.getValue("Download.Site");
		processDirectoryName = cfgObject.getValue("processDirectory");

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

	public void downloadFile() {
		// String URL =
		// "https://file-examples-com.github.io/uploads/2017/10/file-sample_150kB.pdf";
		String fullURL = "https://b-ok.cc/book/5517559/8ae8bf";

		Response res = null;
		Document doc = null;

		try {
			res = Jsoup.connect(fullURL).header("Accept-Encoding", "epub")
					.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
					.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
					.userAgent(
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
					.execute();

			doc = res.parse();
			String someURL = doc.attr("abs:href");

			byte[] content = res.bodyAsBytes();

			File outFile = new File("S:\\Ulvi\\wordspace\\Wordlet\\data\\outfile.zip");
			FileOutputStream os = new FileOutputStream(outFile);
			os.write(content);
			os.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processLevel2() {
		String fullURL = null;
		boolean pageFound = false;
		ResultSet records = null;

		int count = 0;

		try {
			records = psSelect.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (records.next()) {
				String word = records.getString(1);
				String source = records.getString(2);
				String id = records.getString(3);

				fullURL = String.format("%s/tr/q/%s-ceviri-nedir", baseURL, word);

				Response res = null;
				Document doc = null;

				res = Jsoup.connect(fullURL).header("Accept-Encoding", "gzip, deflate, br")
						.header("Accept-Language", "en-US,en;q=0.9,de;q=0.8").header("Cache-Control", "max-age=0")
						.header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
						.userAgent(
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")
						.execute();

				doc = res.parse();
				String errorString = String.format("\"%s\" terimi bulunamadı", word);
				boolean errorOccurance = doc.html().contains(errorString);

				Elements content = new Elements();
				String contentString = null;

				if (errorOccurance) {
					contentString = String.format("%s does not exist in zargan.com", word);
				} else {
					content = doc.select("div[id$=resultsContainer]");
					if (content == null) {
						contentString = String.format("%s does not exist in zargan.com", word);
					} else {
						contentString = content.toString();
					}
				}

				logger.info("+++++ Header found in " + fullURL);
				java.util.Date time = new java.util.Date();
				try {
					psInsert.setString(1, word);
					psInsert.setString(2, baseURL);
					psInsert.setString(3, fullURL);
					psInsert.setString(4, contentString);
					psInsert.setString(5, format.format(time));
					psInsert.executeUpdate();
					count++;
					// logger.debug("Inserted record : " + tagItem.toString());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
				count++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void processDirectory() {
		File directory = new File(processDirectoryName);
		if (directory.isDirectory()) {
			processDirectory(directory);
		} else {
			logger.info(String.format("Designated location directory []%s is not a directory, will not process .",
					directory.getAbsolutePath()));
		}
	}

	public void processDirectory(File directory) {
		if (directory.isDirectory()) {

			File[] files = directory.listFiles();
			for (File file : files) {
				String extension = GlobalUtils.getFileExtension(file);

				if (extension == null) {
					logger.info(String.format("Extension for the file %s is not known, cannot process, skipping ..",
							file.getAbsolutePath()));
				} else {
					String upperExtension = extension.toUpperCase();
					switch (upperExtension) {
					case "PDF":
						processPDFFile(file);
						shiftFile(file, extension);
						break;
					case "EPUB":
						processEpubFile(file);
						break;
					default:
						logger.info(String.format("Undefined process for %s, cannot proceed, skipping ..",
								file.getAbsolutePath()));
					}
				}
			}
		}
	}

	private boolean shiftFile(File file, String extension) {
		boolean retval = false;
		File moveDirectory = new File(file.getParentFile(), "_" + extension.toLowerCase());

		if (moveDirectory.isDirectory()) {
			File targetFile = new File(moveDirectory, file.getName());
			try {
				Path result = Files.move(Paths.get(file.getAbsolutePath()), Paths.get(targetFile.getAbsolutePath()));
				if (result != null) {
					logger.info(
							String.format("Moved file %s to %s directory", file.getAbsoluteFile(), result.getParent()));
				} else {
					logger.info(String.format("Could not move file %s", file.getAbsoluteFile()));
					logger.info("Unexpected case, exitting .. !!");
					System.exit(-1);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.info("Unexpected case, exitting-2 .. !!");
				System.exit(-1);
			}
		}
		return retval;
	}

	public void processEpubFile(File file) {
		ZipFile zFile = null;
		try {
			zFile = new ZipFile(file.getAbsoluteFile());

			for (Enumeration<? extends ZipEntry> e = zFile.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				// logger.info(String.format("file [%s] => %s", file.getAbsoluteFile(),
				// entry.getName()));
				if (isContentRelevant(entry)) {
					String content = getContent(zFile, entry);
					WikiLevel2Parser wikiParser = new WikiLevel2Parser();
					TreeSet<String> wordsSet = ledgerManager.cleanseWordlist(content, wikiParser);
					String words = GlobalUtils.convertArrayToString(wordsSet, ", ");
					logger.info("*** " + words + " ***");
					ledgerManager.saveWordListIntoLedgerTable(wordsSet, file.getName());
					ledgerManager.lookupInTDK();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processPDFFile(File file) {
		PDDocument document = null;
		try {
			document = PDDocument.load(file);
			if (!document.isEncrypted()) {
				PDFTextStripperByArea stripper = new PDFTextStripperByArea();
				stripper.setSortByPosition(true);

				PDFTextStripper tStripper = new PDFTextStripper();

				String content = tStripper.getText(document);
				// content = content.substring(0, 1000);
				// System.out.println("Text:" + st);

				WikiLevel2Parser wikiParser = new WikiLevel2Parser();
				TreeSet<String> wordsSet = ledgerManager.cleanseWordlist(content, wikiParser);
				String words = GlobalUtils.convertArrayToString(wordsSet, ", ");
				logger.info("*** " + words + " ***");
				ledgerManager.saveWordListIntoLedgerTable(wordsSet, file.getName());
				// ledgerManager.lookupInTDK();
			} else {
				logger.info(String.format("Document [%s] cannot be processed as it is encrypted !!",
						file.getAbsolutePath()));
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			document.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getContent(ZipFile zipFile, ZipEntry zipEntry) {
		String retval = null;
		InputStream is = null;
		long size = zipEntry.getSize();
		byte[] bytes = null;
		try {
			is = zipFile.getInputStream(zipEntry);
			bytes = new byte[(int) size];
			is.read(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		retval = new String(bytes, StandardCharsets.UTF_8);
		return retval;
	}

	private boolean isContentRelevant(ZipEntry entry) {
		boolean retval = false;

		String entryName = entry.getName();
		String type = new MimetypesFileTypeMap().getContentType(entryName);
		String extension = GlobalUtils.getFileExtension(entryName);

		if ("application/octet-stream".equals(type)) {
			if (extension.equals("xhtml") || extension.equals("html")) {
				retval = true;
			}
		} else if ("image/gif".equals(type)) {
			retval = false;
		} else if ("image/jpeg".equals(type)) {
			retval = false;
		} else if ("text/plain".equals(type)) {
			retval = false;
		}
		return retval;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		ZLibraryScrapeAndInsert instance = new ZLibraryScrapeAndInsert(args[0]);
		// instance.processLevel1();
		// instance.processEpubFiles();
		// instance.processPDFFiles();
		// instance.downloadFile();
		ConfigReader cfg = new ConfigReader(args[0]);

		String runMethodName = cfg.getValue("runMethod");
		String[] params = ReflectionUtils.removeFirstElement(args);

		Class<?> instanceClass = null;
		Method method = null;

		try {
			Class[] argTypes = ReflectionUtils.getParameterClassArray(params);
			instanceClass = Class.forName("com.langpack.integration.ZLibraryScrapeAndInsert");
			method = instanceClass.getMethod(runMethodName, argTypes);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			Object[] args2 = params;
			logger.info("***** Invoking : " + instanceClass.getName() + "." + method.getName()
					+ method.getParameterTypes());
			method.invoke(instance, args2);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
