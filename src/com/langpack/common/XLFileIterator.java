package com.langpack.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLFileIterator {
	public static final Logger log4j = LogManager.getLogger("FileIterator");

	int FILE_VERSION = 0;
	String importFileBase = null;
	String[] importExtensionsList = new String[] { "xlsx" };
	String importFileExtension = "xlsx";
	File inputFile = null;
	File nextFile = null;
	File importDir = null;
	FileInputStream fsIP = null;
	XSSFWorkbook wb = null;

	int saveFrequency = 1;

	public XLFileIterator(File _importDir) {
		this.importDir = _importDir;
		this.importFileBase = findFileBase(this.importDir);
	}

	public XLFileIterator() {

	}

	public int getSaveFrequency() {
		return saveFrequency;
	}

	public void setSaveFrequency(int saveFrequency) {
		this.saveFrequency = saveFrequency;
	}

	public void saveCheck(int rowCount) {
		if (rowCount % saveFrequency == 0) {
			log4j.info("AutoSaving at line " + rowCount);
			saveFile();
		}
	}

	private String findFileBase(File importDir) {
		String KEY_PATTERN = "_v1.";
		String retval = null;
		File[] files = importDir.listFiles();
		for (File tmp : files) {
			String tmpName = tmp.getName();
			if (tmpName.contains(KEY_PATTERN)) {
				retval = tmpName.substring(0, tmpName.indexOf(KEY_PATTERN));
				retval = retval + KEY_PATTERN;
				break;
			}
		}
		return retval;
	}

	public File getNextImportFile() {
		String tmpFilename = null;
		int count = 0;
		if (inputFile == null) {
			while (true) {
				tmpFilename = importFileBase + count + "." + importFileExtension;
				File tmpFile = new File(importDir, tmpFilename);
				if (tmpFile.exists()) { // if file exists and not searching for the first one
					log4j.info("Already exists : " + tmpFile.getName());
					FILE_VERSION = count;
					count++;
				} else { // if file does not exist
					log4j.info("Next file identified  : " + tmpFile.getName());
					break;
				}
			}
		}

		tmpFilename = importFileBase + FILE_VERSION + "." + importFileExtension;
		inputFile = new File(importDir, tmpFilename);

		FILE_VERSION++;
		tmpFilename = importFileBase + FILE_VERSION + "." + importFileExtension;
		nextFile = new File(importDir, tmpFilename);

		try {
			com.google.common.io.Files.copy(inputFile, nextFile);
			log4j.info(String.format("Copied %s -> %s", inputFile, nextFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		inputFile = nextFile;

		return inputFile;
	}

	public void saveFile() {
		String inputFileName = inputFile.getAbsolutePath();
		String bakFileName = inputFileName.replace(importFileExtension, "bak");
		File bakFile = new File(bakFileName);

		try {
			log4j.info("Taking backup of the old file : " + bakFileName);
			com.google.common.io.Files.copy(inputFile, bakFile);
			log4j.info("Backup completed OK.");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		log4j.info("Saving data file : " + inputFile);
		try {
			fsIP.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Open FileOutputStream to write updates
		FileOutputStream output_file = null;
		try {
			output_file = new FileOutputStream(inputFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// write changes
		try {
			wb.write(output_file);
			output_file.close();
			log4j.info("Saved data file : " + inputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public XSSFSheet getSheet(String sheetName) {
		XSSFSheet retval = null;
		openWorkbook();
		retval = wb.getSheet(sheetName);
		return retval;
	}

	public XSSFWorkbook openWorkbook() {
		if (wb == null) {
			try {
				fsIP = new FileInputStream(inputFile);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// Access the workbook
			log4j.info(String.format("Loading file %s ..", inputFile.getAbsoluteFile()));
			try {
				wb = new XSSFWorkbook(fsIP);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return wb;
	}

	// no iteration, open the current file
	public XSSFWorkbook openWorkbook(File tmpFile) {
		inputFile = tmpFile;
		try {
			fsIP = new FileInputStream(inputFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Access the workbook
		log4j.info(String.format("Loading file %s ..", inputFile.getAbsoluteFile()));
		try {
			wb = new XSSFWorkbook(fsIP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return wb;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		String importListPath = args[0];

		// Using an importlist text file to find all the files which need to be
		// processed
		TextFileReader reader = new TextFileReader(importListPath);
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				if (!line.substring(0, 1).equals("#")) {
					File importDir = new File(line);
					XLFileIterator fileIter = new XLFileIterator(importDir);
					File nextFile = fileIter.getNextImportFile();
					XSSFWorkbook wb = fileIter.openWorkbook();
					XSSFSheet sheet = wb.getSheetAt(0);
					String name = sheet.getSheetName();
					log4j.info("Name : " + name);

				} else {
					log4j.info("Skipping comment line " + line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
