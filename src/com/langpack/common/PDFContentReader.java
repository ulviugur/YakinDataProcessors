package com.langpack.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFContentReader {
	public static final Logger logger = LogManager.getLogger("PDFTest");
	private RandomAccessBufferedFileInputStream fis;
	private File readFile = null;

	public PDFContentReader(int count, File inFile) {
		readFile = inFile;
		try {
			if (!readFile.exists()) {
				logger.error("[{}] File {} does not exist", count, readFile.getAbsolutePath());
				return;
			}
			fis = new RandomAccessBufferedFileInputStream(readFile);
		} catch (IOException e) {
			logger.error("Error initializing file input stream", e);
		}
	}

	@SuppressWarnings("unused")
	public String analyzeTextFollowingDashes(String intext) {

		
		StringBuilder sb = new StringBuilder();
		if (intext == null || intext.isEmpty()) {
			logger.error("Input text is null or empty.");

			return null;
		}
		logger.debug("Processing string {} ", intext.length());

		int length = intext.length();
		int index = 0;

		while (index < length) {
			String cursor = intext.substring(index, index + 1);
			int value = (int)(cursor.charAt(0));
			
			if (index == 126061) {
				logger.info("Achieved ..");
			}
			
			logger.debug("Processing file index {}, character {} ", index, value);
			
			Pattern patternNumber = Pattern.compile("\\d+");
			Matcher matcherNumber = patternNumber.matcher(cursor);
			
			if ("-".equals(cursor)) {
				// Check for next characters to match patterns
				int start = index; // Start at the dash
				int end = Math.min(start + 5, length); // Check up to 7 characters after the dash
				String followingChars = intext.substring(start, end);

				Pattern patternDashWithPageNumber = Pattern.compile("-\\r?\\n\\d+");
				Matcher matcherDashWithPageNumber = patternDashWithPageNumber.matcher(followingChars);

				Pattern patternDashWithNewline = Pattern.compile("-\\r?\\n");
				Matcher matcherDashWithNewline = patternDashWithNewline.matcher(followingChars);

				if (matcherDashWithPageNumber.find()) {
					String foundPattern = matcherDashWithPageNumber.group();
					logger.debug("Matched '-\\r?\\n\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				} else if (matcherDashWithNewline.find()) {
					String foundPattern = matcherDashWithNewline.group();
					logger.debug("Matched '-\\r?\\n' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				} else {
					logger.debug("No pattern matched");
					// Move to the next character
					index++;
				}
			} else if ("\r".equals(cursor)) {
				int start = index; // Start at the dash
				int end = Math.min(start + 7, length); // Check up to 7 characters after the dash
				String followingChars = intext.substring(start, end);

				Pattern patternDashWithPageNumber = Pattern.compile("\\r\\n\\d+");
				Matcher matcherDashWithPageNumber = patternDashWithPageNumber.matcher(followingChars);
				
				Pattern patternNewline = Pattern.compile("\\r\\n");
				Matcher matcherNewline = patternNewline.matcher(followingChars);
				
				Pattern patternCarriageReturnNoNewline = Pattern.compile("\\r|\\r\\d+");
				Matcher matcherCarriageReturnNoNewline = patternCarriageReturnNoNewline.matcher(followingChars);
				
				if (matcherDashWithPageNumber.find()) {
					String foundPattern = matcherDashWithPageNumber.group();
					logger.debug("Matched '\\r\\n\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				} else if (matcherNewline.find()) {
					String foundPattern = matcherNewline.group();
					logger.debug("Matched '\\r\\n' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				} else if (matcherCarriageReturnNoNewline.find()) {
					String foundPattern = matcherCarriageReturnNoNewline.group();
					logger.debug("Matched '\\r|\\r\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				}

			} else if ("\n".equals(cursor)) {
				int start = index; // Start at the dash
				int end = Math.min(start + 7, length); // Check up to 7 characters after the dash
				String followingChars = intext.substring(start, end);

				Pattern patternNewline = Pattern.compile("\\n|\\n\\d+");
				Matcher matcherNewline = patternNewline.matcher(followingChars);

				if (matcherNewline.find()) {
					String foundPattern = matcherNewline.group();
					logger.debug("Matched '\\n' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				}

			} else if (matcherNumber.find()) {
				int start = index; // Start at the dash
				int end = Math.min(start + 7, length); // Check up to 7 characters after the dash
				String followingChars = intext.substring(start, end);

				Pattern patternNumberdot = Pattern.compile("\\d+|\\d+\\.");
				Matcher matcherNumberdot = patternNumberdot.matcher(followingChars);

				if (matcherNumberdot.find()) {
					String foundPattern = matcherNumberdot.group();
					logger.debug("Matched '\"\\d+|\\d+\\.\"' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				}

			} else {
				sb.append(cursor);
				index++;
				//logger.info("New text : [[ {} ]]", accText.toString());
			}
		}

		String textContent = sb.toString();
		
		logger.info("Ended reading, ended with length {}. Now removing empty spaces.. ", textContent.length());
		
        String retval = StringProcessor.cleanBookString(textContent.toString());
        return retval; // Trim to remove any trailing new lines
	}

	public String readContent() {
		if (fis == null) {
			logger.error("File input stream is not initialized");
			return null;
		}

		String content = null;
		PDFParser parser = null;
		PDDocument doc = null;

		try {
			parser = new PDFParser(fis);
			parser.parse();
			doc = parser.getPDDocument();
			PDFTextStripper stripper = new PDFTextStripper();
			content = stripper.getText(doc);

		} catch (IOException e) {
			logger.error("Error reading content from PDF", e);
		} finally {
			// Close PDDocument if it was successfully opened
			if (doc != null) {
				try {
					doc.close();
				} catch (IOException e) {
					logger.error("Error closing PDDocument", e);
				}
			}
			// Since PDFParser does not implement AutoCloseable, no need to close it
			// explicitly
		}
		return content;
	}

}
