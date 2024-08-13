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
	private FileExporter fex = null;

	public PDFContentReader(int count, File readFile, File exportFile) {
		try {
			if (!readFile.exists()) {
				logger.error("[{}] File {} does not exist", count, readFile.getAbsolutePath());
				return;
			}
			fis = new RandomAccessBufferedFileInputStream(readFile);
		} catch (IOException e) {
			logger.error("Error initializing file input stream", e);
		}

		try {
			if (exportFile != null) {
				fex = new FileExporter(exportFile);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public String analyzeTextFollowingDashes(String intext) {
		if (intext == null || intext.isEmpty()) {
			System.out.println("Input text is null or empty.");
			return intext;
		}

		int length = intext.length();
		int index = 0;

		while (index < length) {
			String cursor = intext.substring(index, index + 1);

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
					logger.info("No pattern matched");
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
				
				if (matcherDashWithPageNumber.find()) {
					String foundPattern = matcherDashWithPageNumber.group();
					logger.debug("Matched '\\r\\n\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				} else if (matcherNewline.find()) {
					String foundPattern = matcherNewline.group();
					logger.debug("Matched '-\\r?\\n\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				}

			} else if ("\n".equals(cursor)) {
				int start = index; // Start at the dash
				int end = Math.min(start + 7, length); // Check up to 7 characters after the dash
				String followingChars = intext.substring(start, end);

				Pattern patternNewline = Pattern.compile("\\r\\n|\r");
				Matcher matcherNewline = patternNewline.matcher(followingChars);

				if (matcherNewline.find()) {
					String foundPattern = matcherNewline.group();
					logger.debug("Matched '-\\r?\\n\\d+' pattern : ['{}']", foundPattern);
					index += foundPattern.length();
				}

			} else {
				if (fex != null) {
					fex.writeStringToFile(cursor);
				}
				index++;
				//logger.info("New text : [[ {} ]]", accText.toString());
			}
		}

		return null;
	}

	private String applyFilters(String rawString) {
		return correctSentenceEnds(rawString);
	}

	private String correctSentenceEnds(String intext) {
		if (intext == null || intext.isEmpty()) {
			return intext;
		}

		// Use a StringBuilder for efficient text manipulation
		StringBuilder correctedText = new StringBuilder(intext);
		int length = correctedText.length();
		int index = 0;

		while (index < length) {
			char currentChar = correctedText.charAt(index);

			if (currentChar == '-') {
				// Check for "-\r\n" or "-\n" patterns
				if (index + 1 < length
						&& (correctedText.charAt(index + 1) == '\r' || correctedText.charAt(index + 1) == '\n')) {
					if (index + 2 < length && correctedText.charAt(index + 2) == '\n') {
						// Handle "-\r\n" or "-\n" pattern
						correctedText.deleteCharAt(index); // Remove dash
						correctedText.replace(index, index + 2, " "); // Replace with space
						length -= 2; // Adjust length due to replacement
					} else {
						// Handle "-\r" or "-\n" pattern
						correctedText.deleteCharAt(index); // Remove dash
						correctedText.replace(index, index + 1, " "); // Replace with space
						length -= 1; // Adjust length due to replacement
					}
				}
			}

			// Move to the next character
			index++;
		}

		// Optional: Trim any leading or trailing whitespace
		return correctedText.toString().trim();
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
