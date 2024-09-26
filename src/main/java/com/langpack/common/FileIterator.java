package com.langpack.common;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileIterator {
	public static final Logger logger = LogManager.getLogger("FileIterator");

	String sourceFileBase = null;
	String sourceFileExtension = null;
	File sourceDir = null;
	File currFile = null;
	File[] processFileList = null;
	boolean IS_DIRECTORY = false;
	int fileCursor = -1;

	public File getCurrFile() {
		return currFile;
	}
	
	public FileIterator(File sourceFile) throws InvalidRequestException {
		if (sourceFile.isFile() || !sourceFile.exists()) {
			this.currFile = sourceFile;
			this.sourceDir = sourceFile.getParentFile();
			if (this.sourceDir == null) {
				this.sourceDir = new File("./");
			}
			this.sourceFileExtension = GlobalUtils.getFileExtension(sourceFile);
			this.sourceFileBase = GlobalUtils.getFileBase(sourceFile);
		} else if (sourceFile.isDirectory()) {
			IS_DIRECTORY = true;
			processFileList = sourceFile.listFiles();
			this.currFile = moveToNextTarget();
			logger.info("Moved to the first file : {}", currFile);
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
						this.currFile = targetFile;
						this.sourceDir = targetFile.getParentFile();
						this.sourceFileExtension = GlobalUtils.getFileExtension(targetFile);
						this.sourceFileBase = GlobalUtils.getFileBase(targetFile);
						return this.currFile;
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


	/*
	 * private String findFileBase(File importDir) { String KEY_PATTERN = "_v1.";
	 * String retval = null; File[] files = importDir.listFiles(); for (File tmp :
	 * files) { String tmpName = tmp.getName(); if (tmpName.contains(KEY_PATTERN)) {
	 * retval = tmpName.substring(0, tmpName.indexOf(KEY_PATTERN)); retval = retval
	 * + KEY_PATTERN; break; } } return retval; }
	 */

	public File getNextFile() {
				
		String KEY_PATTERN = "_v1.";
		
		File nextFile = null;
		int FILE_VERSION = 0;
		String tmpFilename = null;
		int count = 0;

		while (true) {
			tmpFilename = sourceFileBase  + KEY_PATTERN + count + "." + sourceFileExtension;
			File tmpFile = new File(sourceDir, tmpFilename);
			if (tmpFile.exists()) { // if file exists and not searching for the first one
				logger.debug("Already exists : " + tmpFile.getName());
				FILE_VERSION = count;
				count++;
			} else { // if file does not exist
				logger.debug("Next file identified  : " + tmpFile.getName());
				nextFile = tmpFile;
				break;
			}
		}

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
