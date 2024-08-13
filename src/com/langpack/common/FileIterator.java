package com.langpack.common;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileIterator {
	public static final Logger logger = LogManager.getLogger("FileIterator");

	String sourceFileBase = null;
	String sourceFileExtension = null;
	File sourceDir = null;
	File[] processFileList = null;
	boolean IS_DIRECTORY = false;
	int fileCursor = -1;

	public FileIterator(File targetFile) throws InvalidRequestException {
		if (targetFile.isFile()) {
			this.sourceDir = targetFile.getParentFile();
			this.sourceFileExtension = findFileExtension(targetFile);
			this.sourceFileBase = findFileBase(targetFile, sourceFileExtension);
		} else if (targetFile.isDirectory()) {
			IS_DIRECTORY = true;
			processFileList = targetFile.listFiles();
			File tmpNext = moveToNextTarget();
			logger.info("Moved to the first file : {}", tmpNext);
		} else {
			throw new InvalidRequestException("Target file is not valid !!");
		}
	}

	public File moveToNextTarget() throws InvalidRequestException {
		if (IS_DIRECTORY) {
			while (true) {
				fileCursor++;
				if (processFileList.length > fileCursor) {
					File targetFile = processFileList[fileCursor];
					if (targetFile.isDirectory()) {
						// Skip processing deeper directories

					} else if (targetFile.isFile()) {
						this.sourceDir = targetFile.getParentFile();
						this.sourceFileExtension = findFileExtension(targetFile);
						this.sourceFileBase = findFileBase(targetFile, sourceFileExtension);
						return targetFile;
					}
				} else {
					return null;
				}

			}
		} else {
			throw new InvalidRequestException(
					"Invalid call as the target is a file. moveToNextFile() is only valid for Directory processing.");
		}
	}

	public static String findFileExtension(File sourceFile) {
		String retval = null;
		String name = sourceFile.getName();
		String[] parts = name.split("\\.");
		if (parts.length > 1) {
			retval = parts[parts.length - 1];
		}

		return retval;
	}

	public static String findFileBase(File sourceFile, String extension) {
		String KEY_PATTERN = "_v1.";
		String retval = null;

		String sourcefileName = sourceFile.getName();
		if (extension != null) {

		}
		if (sourcefileName.contains(KEY_PATTERN)) {
			logger.info("The file  {} already has a version number", sourcefileName);
			retval = sourcefileName.substring(0, sourcefileName.indexOf(KEY_PATTERN));
			retval = retval + KEY_PATTERN;
		} else {
			logger.info("The file {} doesn't have a version number", sourcefileName);
			retval = sourcefileName + KEY_PATTERN;
		}

		return retval;
	}

	/*
	 * private String findFileBase(File importDir) { String KEY_PATTERN = "_v1.";
	 * String retval = null; File[] files = importDir.listFiles(); for (File tmp :
	 * files) { String tmpName = tmp.getName(); if (tmpName.contains(KEY_PATTERN)) {
	 * retval = tmpName.substring(0, tmpName.indexOf(KEY_PATTERN)); retval = retval
	 * + KEY_PATTERN; break; } } return retval; }
	 */

	public File getNextFile() {
		File nextFile = null;
		int FILE_VERSION = 0;
		String tmpFilename = null;
		int count = 0;

		while (true) {
			tmpFilename = sourceFileBase + count + "." + sourceFileExtension;
			File tmpFile = new File(sourceDir, tmpFilename);
			if (tmpFile.exists()) { // if file exists and not searching for the first one
				logger.info("Already exists : " + tmpFile.getName());
				FILE_VERSION = count;
				count++;
			} else { // if file does not exist
				logger.info("Next file identified  : " + tmpFile.getName());
				break;
			}
		}

		FILE_VERSION++;
		tmpFilename = sourceFileBase + FILE_VERSION + "." + sourceFileExtension;
		nextFile = new File(sourceDir, tmpFilename);

		return nextFile;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		String sourceDirPath = args[0];
		File source = new File(sourceDirPath);
		FileIterator fileIter = null;
		if (source.isDirectory()) { // Source is a directory, iterate through all files
			try {
				fileIter = new FileIterator(source);

				do {
					File nextFile = fileIter.getNextFile();
					logger.info("Next Filename : " + nextFile.getName());
				} while (fileIter.moveToNextTarget() != null);

			} catch (InvalidRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (source.isFile()) {

			try {
				fileIter = new FileIterator(source);
				File nextFile = fileIter.getNextFile();
				logger.info("Next Filename : " + nextFile.getName());
			} catch (InvalidRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
