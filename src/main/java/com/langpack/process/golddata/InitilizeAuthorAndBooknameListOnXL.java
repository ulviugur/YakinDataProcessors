package com.langpack.process.golddata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.LevensteinIndex;
import com.langpack.common.StringProcessor;
import com.langpack.common.XLFileIterator;
import com.langpack.datachannel.UnknownDataChannelException;
import com.langpack.model.LibraryLinkItem;
import com.langpack.process.booktoquote.ProcessDocumentFiles;
import com.langpack.scraper.Qualifier;

public class InitilizeAuthorAndBooknameListOnXL {

	public static final Logger log4j = LogManager.getLogger("ConsileBookNamesOnXL");

	XSSFWorkbook wb = null;

	XLFileIterator xlIter = null;

	String processSheetName = null;
	XSSFSheet processSheet = null;
	String goldSheetName = null;
	XSSFSheet goldSheet = null;

	Boolean importData = false;
	Boolean pageFound = false;

	String sourceDirStr = null;
	File sourceDir = null;

	String sourceFileStr = null;
	String targetFileStr = null;
	int skipLines = 1;

	File sourceFile = null;

	protected ConfigReader cfg = null;

	HashSet<String> existingKeys = new HashSet<String>();

	HashMap<String, String> booksMap = new HashMap<String, String>();
	XSSFCellStyle styleOK = null;
	XSSFCellStyle styleChanged = null;
	XSSFCellStyle styleUnknown = null;

	String chromeConfig = null;
	BookDataManager correctUtil = null;

	String NA = "NA";

	public InitilizeAuthorAndBooknameListOnXL(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);

		chromeConfig = cfg.getValue("ChromeHeadlessConfigFile");

		sourceDirStr = cfg.getValue("Process.Workbookdir");
		sourceDir = new File(sourceDirStr);

		processSheetName = cfg.getValue("Process.Sheetname");
		goldSheetName = cfg.getValue("Process.GoldSheet");

		skipLines = Integer.parseInt(cfg.getValue("Process.SkipLines", "1"));

		try {
			xlIter = new XLFileIterator(sourceDir);
			sourceFile = xlIter.getNextImportFile();
			wb = xlIter.openWorkbook();
			processSheet = wb.getSheet(processSheetName);
			goldSheet = wb.getSheet(goldSheetName);
			if (processSheet == null) {
				processSheet = wb.createSheet(processSheetName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadCurrentAuthors(goldSheet);
		styleOK = wb.createCellStyle();
		styleOK.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		styleOK.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		styleChanged = wb.createCellStyle();
		styleChanged.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		styleChanged.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		styleUnknown = wb.createCellStyle();
		styleUnknown.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		styleUnknown.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		correctUtil = new BookDataManager(chromeConfig);
	}

	public void loadCurrentAuthors(XSSFSheet _sheet) {
		try {
			int lineCount = 1;
			XSSFRow rowObject = null;

			while (true) {
				// log4j.info("Line [{}]", lineCount);
				rowObject = (XSSFRow) _sheet.getRow(lineCount);
				if (rowObject == null) {
					break;
				}

				Cell cellStrippedAuthors = rowObject.getCell(2);
				String strippedAuthors = GlobalUtils.getCellContentAsString(cellStrippedAuthors);

				Cell cellVerifiedAuthorsOld = rowObject.getCell(3);
				String currAuthorValue = GlobalUtils.getCellContentAsString(cellVerifiedAuthorsOld);
				if (currAuthorValue != null && !"".equals(currAuthorValue)) {
					booksMap.put(strippedAuthors, currAuthorValue);
				}

				lineCount++;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private HashMap<String, String> searchAuthorInCachedBooks(String bookName, String author) {
		HashMap<String, String> retval = null;
		Iterator<String> iter = booksMap.keySet().iterator();
		String foundAuthor = null;
		while (iter.hasNext()) {
			String item = iter.next();
			String writer = booksMap.get(item);
			String checkedAuthor = checkAuthorName(author, writer);
			if (checkedAuthor != null) {
				foundAuthor = checkedAuthor;
			}
		}

		return retval;
	}

	private String checkAuthorName(String _oriAuthor, String _foundAuthor) {

		String retval = null;

		String cleanOriAuthor = _oriAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");
		String cleanFoundAuthor = _foundAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");

		// BOF - CASE1
		String oriAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanOriAuthor);
		String foundAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanFoundAuthor);

		String[] partsOriAuthor = oriAuthor.split(" ");
		String[] partsFoundAuthor = foundAuthor.split(" ");

		Set<String> setOriAuthor = new TreeSet<>(Arrays.asList(partsOriAuthor));
		Set<String> setFoundAuthor = new TreeSet<>(Arrays.asList(partsFoundAuthor));

		int matchCount = matchingKeys(setOriAuthor, setFoundAuthor);

		int dist = LevensteinIndex.distance(GlobalUtils.convertArrayToString(setOriAuthor),
				GlobalUtils.convertArrayToString(setFoundAuthor));

		if (dist <= partsFoundAuthor.length) { // also tried with matchCount >= 2; that brings false positives, probably
												// eliminates some
			retval = _foundAuthor;
		}

		return retval;
	}

	private int matchingKeys(Set<String> original, Set<String> found) {
		// Count how many words from the original set are found in the found set
		int matchCount = 0;
		for (String word : original) {
			if (found.contains(word)) {
				matchCount++;
			}
		}

		// Calculate and return the percentage of words that matched
		return matchCount;
	}

	public void runSecond() {
		log4j.info("Run second started, correcting only authors ..");
		int rowCount = skipLines;
		//int rowCount = 1637;
		XSSFRow rowObject = null; // skip title row

		while (true) {
			System.out.print(".");
			rowObject = (XSSFRow) processSheet.getRow(rowCount);
			if (rowObject == null) {
				break;
			}
			Cell cellOriBookname = rowObject.getCell(3);
			String oriBookname = GlobalUtils.getCellContentAsString(cellOriBookname);

			Cell cellOriAuthor = rowObject.getCell(4);
			String oriAuthor = GlobalUtils.getCellContentAsString(cellOriAuthor);

			Cell cellConfirmedAuthor = rowObject.getCell(6);
			String currConfirmedAuthor = GlobalUtils.getCellContentAsString(cellConfirmedAuthor);
			
			// already filled
			if (!(currConfirmedAuthor == null || "".equals(currConfirmedAuthor))) {
				rowCount++;
				continue;
			}
			
			// !! EDITING BLOCK !!
			// etc. -> Kolektif
			cellConfirmedAuthor = rowObject.createCell(6);
			if (oriAuthor.toUpperCase().equals("ETC.")) {
				cellConfirmedAuthor.setCellValue("Kolektif");
				cellConfirmedAuthor.setCellStyle(styleChanged);
				rowCount++;
				continue;
			}
			
			if (oriAuthor.toUpperCase(StringProcessor.turkishLocale).equals("KOLEKTİF")) {
				cellConfirmedAuthor.setCellValue("Kolektif");
				cellConfirmedAuthor.setCellStyle(styleChanged);
				rowCount++;
				continue;
			}
			
			if (oriAuthor.contains("ignore") || (currConfirmedAuthor != null && currConfirmedAuthor.contains("ignore"))) {
				cellConfirmedAuthor.setCellValue(oriAuthor);
				rowCount++;
				continue;
			}

			
			HashMap<String, String> foundRecords = null;
			if (booksMap.get(oriBookname) != null) { // check if the bookname is already in the sheet
				foundRecords = new HashMap<String, String>();
				foundRecords.put(oriBookname, booksMap.get(oriBookname));
			} else { // exact bookname is not there, look for the author
				foundRecords = searchAuthorInCachedBooks(oriBookname, oriAuthor);
			}

			if (foundRecords != null) { // something was found in the current Goldbooks
				String item = foundRecords.keySet().iterator().next();
				String author = foundRecords.get(item);

				if (NA.equals(author)) {
					cellConfirmedAuthor.setCellStyle(styleUnknown);
				} else {
					cellConfirmedAuthor.setCellValue(author);
					if (oriAuthor.equals(author)) {
						cellConfirmedAuthor.setCellStyle(styleOK);
					} else {
						cellConfirmedAuthor.setCellStyle(styleChanged);
					}
				}
			} else {
				log4j.info("Nothing was found in the existing gold records");
				
				String[] authorParts = oriAuthor.split(",");

				ArrayList<String> foundAuthors = new ArrayList<String>();
				for (int i = 0; i < authorParts.length; i++) {
					String authPart = authorParts[i].trim();
					String authorLookup = correctUtil.validateAuthorOnDNR(authPart); // first check on DNR
					if (authorLookup == null) {
						authorLookup = correctUtil.validateAuthorOn1000Kitap(authPart); // not found, try 1000Kitap
					}
					if (authorLookup != null) {
						foundAuthors.add(authorLookup);
					}
					
				}
				String foundAuthorsStr = null;
				if (foundAuthors.size() > 0) {
					foundAuthorsStr = GlobalUtils.convertArrayToString(foundAuthors, ", ");
					cellConfirmedAuthor.setCellValue(foundAuthorsStr);
				}

				if (oriAuthor.equals(foundAuthorsStr)) {
					cellConfirmedAuthor.setCellStyle(styleOK);
				} else {
					cellConfirmedAuthor.setCellStyle(styleChanged);
				}
			}
			rowCount++;
		}
		saveFile();
		log4j.info("Run second completed ..");
	}

	public void runFirst() {

		int rowCount = skipLines;
		XSSFRow rowObject = null; // skip title row

		while (true) {

			rowObject = (XSSFRow) processSheet.getRow(rowCount);
			if (rowObject == null) {
				break;
			}
			Cell cellOriFilename = rowObject.getCell(0);
			String oriFilename = GlobalUtils.getCellContentAsString(cellOriFilename);

			String cleanName = ProcessDocumentFiles.cleanBaseFilename(oriFilename);

			Cell cellCleanName = rowObject.createCell(1);
			cellCleanName.setCellValue(cleanName);

			String id = BookDataConsolidator.getKey(cleanName);

			Cell cellId = rowObject.createCell(2);
			cellId.setCellValue(id);

			if (id == null) {
				id = "";
			}

			String plainName1 = cleanName.replace(id, "");
			String plainName2 = plainName1.replaceAll("\\(.*\\)", "");
			String plainName3 = plainName2.replaceAll("_", " ");
			String plainName4 = plainName3.replaceAll("\\.epub", "");
			String plainName5 = GlobalUtils.toCamelCase(plainName4);

			String plainName6 = uppercaseSingleLetters(plainName5);

			Cell cellPlainName = rowObject.createCell(3);
			cellPlainName.setCellValue(plainName6.trim());

			Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
			Matcher matcher = pattern.matcher(cleanName);

			String authGroup = "";
			// Find and print all matches
			while (matcher.find()) {
				authGroup = matcher.group(1); // Group 1 contains the text between parentheses
				authGroup = authGroup.replaceAll(",", "");
				authGroup = authGroup.replaceAll("_", " ");
				authGroup = authGroup.replaceAll("\\[.*\\]", "");
				authGroup = authGroup.replaceAll("\\[.*", "");
				authGroup = GlobalUtils.toCamelCase(authGroup);

				// Replace the matched pattern with your replacement text
				authGroup = authGroup.replaceAll("-.*Yay.*", "");
				authGroup = authGroup.replaceAll("-.*Kitap.*", "");

				authGroup = authGroup.replace("╥", "E");
				if (authGroup.length() > 6) {
					authGroup = authGroup.replaceAll("(?i)etc.", " ");
				}

			}

			authGroup = uppercaseSingleLetters(authGroup);
			Cell cellAuthorName = rowObject.createCell(4);
			cellAuthorName.setCellValue(authGroup.trim());

			rowCount++;
		}
		saveFile();
		log4j.info("Run first completed ..");
	}

	private String uppercaseSingleLetters(String word) {
		Pattern pattern2 = Pattern.compile("\\b([a-z])\\.");
		Matcher matcher2 = pattern2.matcher(word);

		// Use a StringBuilder to build the result with the replacements
		StringBuffer result = new StringBuffer();

		while (matcher2.find()) {
			// Replace each match with the uppercase version of the single letter J. r. r.
			// -> J. R. R.
			matcher2.appendReplacement(result, matcher2.group(1).toUpperCase() + ".");
		}
		matcher2.appendTail(result); // Append any remaining text

		return result.toString();
	}

	private void saveFile() {
		FileOutputStream output_file = null;
		try {
			output_file = new FileOutputStream(sourceFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// write changes
		try {
			wb.write(output_file);
			output_file.close();
			log4j.info("Saved data file : " + sourceFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean checkValidForExport(String bookName, String foundBookName, String oriAuthor, String foundAuthor) {

		// Split the book and author names into arrays of words
		String[] partsOriBookName = bookName.split(" ");
		String[] partsOriAuthor = oriAuthor.split(" ");
		String[] partsFoundBookName = foundBookName.split(" ");
		String[] partsFoundAuthor = foundAuthor.split(" ");

		// Convert arrays to sets for easier comparison
		Set<String> setOriBookName = new HashSet<>(Arrays.asList(partsOriBookName));
		Set<String> setOriAuthor = new HashSet<>(Arrays.asList(partsOriAuthor));
		Set<String> setFoundBookName = new HashSet<>(Arrays.asList(partsFoundBookName));
		Set<String> setFoundAuthor = new HashSet<>(Arrays.asList(partsFoundAuthor));

		// Check if either book name or author has enough overlap to consider it valid
		boolean isBookNameMatching = hasWordOverlap(setOriBookName, setFoundBookName);
		boolean isAuthorMatching = hasWordOverlap(setOriAuthor, setFoundAuthor);

		// Return true if either book name or author matches sufficiently
		boolean retval = isBookNameMatching && isAuthorMatching;
		return retval;
	}

	private boolean hasWordOverlap(Set<String> original, Set<String> found) {
		// Check if there's any overlap between the two sets (tolerance for partial
		// matches)
		for (String word : original) {
			if (found.contains(word)) {
				return true; // Found at least one word that matches
			}
		}
		return false; // No matching words found
	}

	private String correctAuthor(String oriAuthor, String foundAuthor) {

		if (oriAuthor.equals("Adıvar Halide Edib")) {
			log4j.info(oriAuthor);
		}
		String[] partsOri = oriAuthor.split(" ");
		String[] partsFound = foundAuthor.split(" ");

		Arrays.sort(partsOri);
		Arrays.sort(partsFound);

		if (Arrays.equals(partsOri, partsFound)) {
			return foundAuthor;
		} else {
			return oriAuthor;
		}
	}

	public static void main(String[] args) {
		// System.out.println("classpath=" + System.getProperty("java.class.path"));

		InitilizeAuthorAndBooknameListOnXL instance;
		instance = new InitilizeAuthorAndBooknameListOnXL(args[0]);
		//instance.runFirst(); runFirst kşlls D and E column correction done manually. Cannot be run in manually corrected sheet
		
		instance.runSecond();

	}

}
