package com.langpack.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GlobalUtils {
	public static String RESPONSE_ZERO_RESULTS = "ZERO_RESULTS";
	public static String RESPONSE_INVALID_REQUEST = "INVALID_REQUEST";
	public static String RESPONSE_OK = "OK";
	// public static String REGEX_WORD_BREAKER="-|\\.|\\(|\\)|\\|,|\\ |'";
	// Removed single quote "'" due to an example Gul'cha in Kirgizistan
	public static String REGEX_WORD_BREAKER = "-|\\.|\\(|\\)|\\|,|\\ ";
	public static String REGEX_COMMA_BREAKER = ",";
	public static String REGEX_NUMBERS = "\\d+";

	public static double TOLERANCE_EASY = 0.85; // KIYA vs. KIA is 0.857
	public static double TOLERANCE_MID = 0.88; // BOUKA vs. BUKA is 0.8888
	public static double TOLERANCE_HARD = 0.91;

	public static String GMAP_KEY = "AIzaSyDGimeDVxhpBy8YF9OMVmaAT58QCPGaEl0";

	private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZçÇğĞıİöÖşŞüÜ ";
	
	public static final Logger log4j = LogManager.getLogger("GlobalUtils");
	public static DecimalFormat COORDS_FORMATTER = new DecimalFormat("##0.000000");

	public static ArrayList<File> checkDirectory(String dirname, ArrayList<String> extlist) {
		ArrayList<File> retval = new ArrayList<>();

		File handleDownloadDir = new File(dirname);

		File[] fileList = handleDownloadDir.listFiles();

		for (File file : fileList) {
			if (file.isDirectory()) {
				// irrelevant
			} else {
				String ext = GlobalUtils.getFileExtension(file).toLowerCase();
				if (extlist.contains(ext)) {
					retval.add(file);
				}
			}
		}

		return retval;
	}

	public static ArrayList<String> mergeArrays(ArrayList<String> array1, ArrayList<String> array2) { // eliminates
																										// duplicates
		ArrayList<String> retval = new ArrayList<>();
		for (int i = 0; i < array1.size(); i++) {
			String tmp = array1.get(i);
			if (!retval.contains(tmp)) {
				retval.add(tmp);
			}
		}
		for (int i = 0; i < array2.size(); i++) {
			String tmp = array2.get(i);
			if (!retval.contains(tmp)) {
				retval.add(tmp);
			}
		}
		return retval;
	}

	public static boolean compareNStrings(String str1, String str2) {
		// compare normalized strings
		boolean retval = false;
		if (normalizeUnicodeStringToUpper(str1).equals(normalizeUnicodeStringToUpper(str2))) {
			retval = true;
		}
		return retval;
	}

	public static String normalizeUnicodeStringToUpper(String tmp) {
		String retval = null;
		if (tmp != null) {
			retval = Normalizer.normalize(tmp, Normalizer.Form.NFD);
			retval = retval.replaceAll("\\p{M}", "");
			retval = retval.toUpperCase();
		}
		return retval;
	}

	public static String normalizeUnicodeStringLeaveCaseAsIs(String tmp) {
		String retval = null;
		if (tmp != null) {
			retval = Normalizer.normalize(tmp, Normalizer.Form.NFD);
			retval = retval.replaceAll("\\p{M}", "");
		}
		return retval;
	}

	public static String callWebsite(String stringURL) {
		String content = callWebsite(stringURL, 10000);
		return content;
	}

	public static String callWebsite(String stringURL, int timeout) {
		Document doc = null;
		String content = null;
		try {
			org.jsoup.Connection conn = Jsoup.connect(stringURL);
			conn.timeout(timeout);
			doc = conn.get();
			content = doc.html();
		} catch (HttpStatusException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return content;
	}

	// escape single quotes for SQL processes
	public static String escapeSpecialCharacters(String value) {
		if (value != null) {
			value = value.replaceAll("'", "''");
		}
		return value;
	}

	public static ArrayList<String> readCountryList(String countryListFileName, Logger log4j) {
		ArrayList<String> countryList = new ArrayList<>();
		int fileReadCount = 0;

		Charset cset = Charset.forName("UTF-8");
		File countryListFile = null;
		FileInputStream fstream = null;
		InputStreamReader freader = null;
		BufferedReader reader = null;
		String line = null;
		String item = null;

		countryListFile = new File(countryListFileName);

		try {
			fstream = new FileInputStream(countryListFile);
			freader = new InputStreamReader(fstream, cset);
			reader = new BufferedReader(freader, 1024);

			while ((line = reader.readLine()) != null) {
				fileReadCount++;
				item = line.replaceAll("\\r|\\n", "");
				countryList.add(item);
				log4j.info(String.format("Added country %s to the process list", item));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info(String.format("Processed %s rows, created %s country items ..", fileReadCount, countryList.size()));
		return countryList;
	}

	public static String stripDescriptiveAndShortKeywords(String data, String[] keywords) {
		String retval = null;
		if (data != null) {
			// remove all keywords which are only descriptive, not specific
			data = GlobalUtils.normalizeUnicodeStringToUpper(data);
			String[] words = data.split(GlobalUtils.REGEX_WORD_BREAKER);
			retval = "";
			for (String word : words) {
				boolean remove = false;
				String current = word.replaceAll(",", "");
				if (current.length() > 2) { // strip small words Like "Al" or "El" or "l'Guardian"
					for (String keyword : keywords) {
						if (current.equals(keyword)) {
							remove = true;
							break;
						}
					}
				} else {
					if (!current.matches(REGEX_NUMBERS)) { // if the word is <= 2 letters and it is not a number, remove
															// it
						remove = true;
					}
				}

				if (!remove) {
					retval += word + " ";
				}
			}
			retval = retval.trim();
		}
		return retval;
	}

	public static String stripDescriptiveKeywords(String data) {
		String retval = null;
		if (data != null) {
			// remove all keywords which are only descriptive, not specific
			String[] keywords = new String[] { "DE", "LA", "AL", "EL", "GOVERNORATE", "GOUVERNORAT", "DISTRICT",
					"DIVISION", "CITE", "TECHNOPOLE" };
			data = GlobalUtils.normalizeUnicodeStringToUpper(data);
			String[] words = data.split(GlobalUtils.REGEX_WORD_BREAKER);
			retval = "";
			for (String word : words) {
				boolean remove = false;
				String current = word.replaceAll(",", "");
				if (current.length() > 2) { // strip small words Like "Al" or "El" or "l'Guardian"
					for (String keyword : keywords) {
						if (current.equals(keyword)) {
							remove = true;
							break;
						}
					}
				} else {
					remove = true;
				}
				if (!remove) {
					retval += word + " ";
				}
			}
			retval = retval.trim();
		}
		return retval;
	}

	public static boolean compareStrings(String str1, String str2) {
		boolean retval = false;
		if (str1 == null && str2 == null) {
			retval = true;
		} else if (str1 != null && str2 != null && str1.equals(str2)) {
			retval = true;
		}
		return retval;
	}

	public static String[] mergeKeywords(String... keys) {
		String[] retval = new String[] {};
		ArrayList<String> tmpArray = new ArrayList<>();
		for (String tmp : keys) {
			if (!tmpArray.contains(tmp) && tmp != null) {
				tmpArray.add(tmp);
			}
		}
		if (tmpArray.size() > 0) {
			retval = new String[tmpArray.size()];
			for (int i = 0; i < retval.length; i++) {
				retval[i] = tmpArray.get(i);
			}
		}
		return retval;
	}

	public static TreeSet<String> getUniqueKeys(String[] tmpArray) {
		TreeSet<String> retval = new TreeSet<>();
		for (String item : tmpArray) {
			retval.add(item);
		}
		return retval;
	}

	public static String convertArraytoString(Collection<String> array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			Iterator<String> iter = array.iterator();
			while (iter.hasNext()) {
				sb.append(iter.next());
				sb.append(separator);
			}
		}
		String retval = sb.toString();
		if (retval.length() > 0) {
			retval = retval.substring(0, retval.length() - 1);
		}
		return retval;
	}

	public static String convertArraytoString(TreeSet<Integer> array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			Iterator<Integer> iter = array.iterator();
			while (iter.hasNext()) {
				sb.append(iter.next());
				sb.append(separator);
			}
		}
		String retval = sb.toString();
		retval = retval.substring(0, retval.length() - 1);
		return retval;
	}

	public static String convertArraytoString(Object[] array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				Object tmpObject = array[i];
				String fld = null;
				if (tmpObject != null) {
					fld = array[i].toString();
				} else {
					fld = "";
				}
				sb.append(fld);
				if (i < array.length - 1) {
					sb.append(separator);
				}
			}
		}
		return sb.toString();
	}

	public static String convertArraytoString(String[] array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				String tmpObject = array[i];
				String fld = null;
				if (tmpObject != null) {
					fld = array[i];
				} else {
					fld = "";
				}
				sb.append(fld);
				if (i < array.length - 1) {
					sb.append(separator);
				}
			}
		}
		return sb.toString();
	}

	public static String convertArraytoString(List<Object> array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			for (int i = 0; i < array.size(); i++) {
				Object tmp = array.get(i);
				String fld = null;
				if (tmp == null) {
					fld = "";
				} else {
					fld = tmp.toString();
				}
				sb.append(fld);
				if (i < array.size() - 1) {
					sb.append(separator);
				}
			}
		}
		return sb.toString();
	}

	public static String convertArraytoString(Collection<String> array) {
		return (convertArraytoString(array, "|"));
	}

	public static String convertArraytoString(Object[] array) {
		return (convertArraytoString(array, "|"));
	}

	public static String convertArraytoString(TreeMap<Integer, String> array) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			for (Object key : array.keySet()) {
				Object value = array.get(key);
				sb.append("{");
				sb.append(key);
				sb.append(",");
				sb.append(value);
				sb.append("}");
			}
		}
		String retval = "[" + sb.toString() + "]";

		return retval;
	}

	public static String convertSettoStringLines(Set<String> input, String endOflineChars) {
		if (endOflineChars == null) {
			endOflineChars = "\r\n";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<String> iter = input.iterator();

		while (iter.hasNext()) {
			String line = iter.next();
			sb.append(line);
			sb.append(endOflineChars);
		}

		return sb.toString();
	}

	public static String getTHABaseName(String value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			String letter = value.substring(i, i + 1);
			if ((letter.toCharArray()[0]) > 1024 || (" ".equals(letter))) {
				sb.append(letter);
			}
		}
		return sb.toString().trim();
	}

	public static String getTHATransBaseName(String value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			String letter = value.substring(i, i + 1);
			if (!"/".equals(letter)) {
				if (!isNumeric(letter) || (" ".equals(letter))) {
					sb.append(letter);
				}
			}
		}
		return sb.toString().trim();
	}

	public static boolean isNumeric(String string) {
		return string.matches("^[-+]?\\d+(\\.\\d+)?$");
	}

	public static String getFileExtension(File file) {
		String name = file.getName();
		return getFileExtension(name);
	}

	public static String getFileExtension(String fullName) {
		int tmpIndex = fullName.lastIndexOf(".");
		if (tmpIndex < 0) {
			return "";
		} else {
			return fullName.substring(tmpIndex + 1);
		}
	}

	public static String getFileBase(String fileName) {
		File tmpFile = new File(fileName);
		String base = getFileBase(tmpFile);
		return base;
	}

	public static String getFileBase(File sourceFile) {
		String fileExtension = getFileExtension(sourceFile);

		String tmpfileName = sourceFile.getName();
		String sourceFileBase = tmpfileName.substring(0, tmpfileName.length() - fileExtension.length() - 1);

		return sourceFileBase;
	}

	public static String replaceWords(String main, String remove, boolean caseSensitive) {
		if (" les ".equals(remove)) {
			System.out.println("");
		}
		String retval = main;
		if (caseSensitive) {
			retval = retval.replaceAll(remove, " ");
		} else {
			String myascii = convertToASCIIUpperCase(main);
			String myremove = convertToASCIIUpperCase(remove);

			Pattern pattern = Pattern.compile(myremove);
			Matcher matcher = pattern.matcher(myascii);
			String tmp = "";

			int[] imagebits = new int[main.length()];
			Arrays.fill(imagebits, 1); // all characters will be used unless turned-off
			// remove all occurrences of the same remove (string)
			while (matcher.find()) {
				int charStart = matcher.start();
				int charEnd = matcher.end();
				for (int i = charStart; i < charEnd; i++) {
					if (i == charEnd - 1 && " ".equals(myremove.substring(myremove.length() - 1))) { // don't remove the
																										// last space,
																										// otherwise
																										// words are
																										// merged :(
						imagebits[i] = 1;
					} else {
						imagebits[i] = 0;
					}
				}
			}
			for (int i = 0; i < imagebits.length; i++) {
				int imagebit = imagebits[i];
				if (imagebit == 1) {
					tmp = tmp.concat(main.substring(i, i + 1));
				}
			}
			retval = tmp.replaceAll("  ", " ");
		}
		return retval;
	}

	private static final String REGEX_ASCII = "[^\\p{ASCII}]";
	private static final Pattern ASCII = Pattern.compile(REGEX_ASCII);
	private static final String REGEX_UPPER = "[\\p{javaUpperCase}&&[^\\p{Upper}]]+";
	private static final Pattern UPPER = Pattern.compile(REGEX_UPPER);

	public static String convertToASCIIUpperCase(String myString) {
		try {
			myString = Normalizer.normalize(myString, Normalizer.Form.NFD);
		} catch (Exception ex) {
			return null;
		}
		myString = myString.toUpperCase();
		myString = myString.replaceAll(REGEX_ASCII, "");
		return myString;
	}

	// find the percentage of how many reference words are found in the
	// compareString array
	public static float getWordMatchLevel(String subset, String superset, boolean caseSensitive) {
		if (!caseSensitive) {
			subset = convertToASCIIUpperCase(subset);
			superset = convertToASCIIUpperCase(superset);
		}
		float retval = (float) 1.0;
		String[] refArray = subset.split(" ");
		String[] compArray = superset.split(" ");
		int found = 0;
		for (String tmp1 : refArray) {
			for (String tmp2 : compArray) {
				if (tmp1.equals(tmp2)) {
					found += 1;
				}
			}
		}
		int divider = refArray.length;
		retval = retval * found / divider;

		return retval;
	}

	public static String toTitleCase(String input) {
		StringBuilder titleCase = new StringBuilder();
		boolean nextTitleCase = true;
		for (char c : input.toCharArray()) {
			if (Character.isSpaceChar(c)) {
				nextTitleCase = true;
			} else if (nextTitleCase) {
				c = Character.toTitleCase(c);
				nextTitleCase = false;
			}
			titleCase.append(c);
		}

		return titleCase.toString();
	}

	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss.SSS");

	public String getTimestamp(String event) {
		java.util.Date time = new java.util.Date();
		return formatter.format(time);
	}

	// convert a String[] to ArrayList<String>; return null if the argument is null
	public static ArrayList<String> toStringArrayList(String[] tmp) {
		ArrayList<String> retval = null;
		if (tmp != null) {
			retval = new ArrayList<>();
			for (String element : tmp) {
				retval.add(element);
			}
		}
		return retval;
	}

	// convert a String[] to ArrayList<String>; return null if the argument is null
	public static String[] toStringArray(ArrayList<String> tmp) {
		String[] retval = new String[tmp.size()];

		for (int i = 0; i < tmp.size(); i++) {
			String item = tmp.get(i);
			retval[i] = item;
		}
		return retval;
	}

	/*
	 * @fileDirectoryPath : searched directory path
	 *
	 * @fileBaseName : root name of the file without version
	 *
	 * @fileExtension : extension of the target file
	 *
	 * @openType : "R" for read "W" for write. If file will be only read, no need to
	 * create a new version, if it will be amended, create a new file
	 *
	 * @returns : Returns the handle of the first file to be created or a new
	 * version of the existing file
	 */
	public static File getNextFile(String fileDirectoryPath, String fileBaseName, String fileExtension,
			String openType) {
		int fileVersion = 0;
		File currentFile = null;
		log4j.info("Getting next file ..");
		File nextFile = null;
		String tmpFilename = null;

		while (true) {
			tmpFilename = fileBaseName + fileVersion + "." + fileExtension;
			File tmpFile = new File(fileDirectoryPath, tmpFilename);
			if (tmpFile.exists()) { // if file exists and not searching for the first one
				log4j.info("Already exists : " + tmpFile.getName());
				currentFile = tmpFile;
				fileVersion++;
			} else { // if file does not exist
				if ("R".equals(openType)) { // read only
					fileVersion--; // go back one file, it exists to read
					tmpFilename = fileBaseName + fileVersion + "." + fileExtension;
					tmpFile = new File(fileDirectoryPath, tmpFilename);
					nextFile = tmpFile;
					log4j.info("Last version file found : " + tmpFile.getName());

				} else {
					nextFile = tmpFile;
					log4j.info("Next file identified  : " + tmpFile.getName());
					if (nextFile != null && fileVersion > 0) { // at least one version of the file is found
						tmpFilename = fileBaseName + fileVersion + "." + fileExtension;
						nextFile = new File(fileDirectoryPath, tmpFilename);
						if ("W".equals(openType)) {
							try {
								com.google.common.io.Files.copy(currentFile, nextFile);
								log4j.info(String.format("Copied %s -> %s", currentFile, nextFile));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				break;
			}
		}
		return nextFile;
	}

	public static int isMemberOfNormalizedSet(String searched, ArrayList<String> targetList) {
		int retval = -1;
		Object[] tmpArray = targetList.toArray();
		for (int i = 0; i < tmpArray.length; i++) {
			boolean compare = compareNStrings((String) tmpArray[i], searched);
			if (compare) {
				retval = i;
				break;
			}
		}
		return retval;
	}

	public static String getCellContentAsCoordsString(Cell tmpCell) {
		String value = null;
		if (tmpCell != null) {
			CellType typeCell = tmpCell.getCellType();
			if (typeCell.equals(CellType.NUMERIC)) {
				double tmpDouble = tmpCell.getNumericCellValue();
				value = COORDS_FORMATTER.format(tmpDouble);
			} else if (typeCell.equals(CellType.STRING)) {
				value = tmpCell.getStringCellValue();
			} else if (typeCell.equals(CellType.FORMULA)) {
				value = tmpCell.getStringCellValue();
			} else if (typeCell.equals(CellType.BLANK)) {
				value = "";
			}
		}
		return value;
	}

	public static String getCellContentAsString(Cell tmpCell) {
		String value = null;
		if (tmpCell != null) {
			CellType typeCell = tmpCell.getCellType();
			if (typeCell.equals(CellType.NUMERIC)) {
				Double tmpDouble = tmpCell.getNumericCellValue();
				Long intPart = (long) tmpDouble.doubleValue();
				if (tmpDouble.doubleValue() == intPart) {
					value = intPart.toString();
				} else {
					value = tmpDouble.toString();
				}

			} else if (typeCell.equals(CellType.STRING)) {
				value = tmpCell.getStringCellValue();
			} else if (typeCell.equals(CellType.FORMULA)) {
				value = tmpCell.getCellFormula();
			} else if (typeCell.equals(CellType.BLANK)) {
				value = "";
			}
		}
		return value;
	}

	public static String getMoreAccentedWord(String test1, String test2) {
		test1 = test1.replaceAll("(^\\h*)|(\\h*$)", "");
		test2 = test2.replaceAll("(^\\h*)|(\\h*$)", "");
		int sum1 = 0;
		int sum2 = 0;
		int letter1 = 0;
		int letter2 = 0;
		try {
			for (int i = 0; i < test1.length(); i++) {
				letter1 = test1.charAt(i);
				letter2 = test2.charAt(i);
				sum1 += letter1;
				sum2 += letter2;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (sum1 > sum2) {
			return test1;
		} else {
			return test2;
		}
	}

	public static String getShiftedCoords(String value, double divider, DecimalFormat coordinateFormat) {
		// divider : higher means less shifting distance
		String retval = null;
		Double lat = null;
		if ("".equals(value) || value == null) {
			retval = "";
		} else {
			try {
				lat = Double.valueOf(value);
				Random rnd = new Random();
				double shifter = rnd.nextDouble() / divider;
				double sign = rnd.nextDouble();
				if (sign > 0.5) {
					sign = 1.0;
				} else {
					sign = -1.0;
				}

				double newLat = lat + shifter * sign;
				retval = coordinateFormat.format(newLat);
			} catch (java.lang.NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return retval;
	}

	// Squeezes all tokens in a specific name for a better comparison, particularly
	// with numbers
	// Converts "Apriliou 1" to "1Apriliou" for a better comparison with "1
	// Apriliou"
	public static String getSimplifiedName(String tmp) {
		String retval = "";
		if (tmp != null) {
			TreeSet<String> keyset = new TreeSet<>();
			String[] splits = tmp.split(" ");
			for (String split : splits) {
				String tmp2 = split;
				tmp2 = tmp2.replaceAll("\\(", "");
				tmp2 = tmp2.replaceAll("\\)", "");
				keyset.add(tmp2);
			}
			Iterator<String> iter = keyset.iterator();
			while (iter.hasNext()) {
				retval = retval + iter.next();
			}
		}
		// log4j.info(String.format("'%s' => '%s'", tmp, retval));
		return retval;
	}

	public static String toCamelCase(String s) {
		if (s != null) {
			String[] parts = s.split(" ");
			String camelCaseString = "";
			for (String part : parts) {
				camelCaseString = camelCaseString + " " + toProperCase(part);
			}
			camelCaseString = camelCaseString.trim();
			return camelCaseString;
		} else {
			return null;
		}
	}

	public static String toProperCase(String s) {
		if (s != null) {
			if (s.length() > 1) {
				return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
			} else {
				return s;
			}
		} else {
			return null;
		}
	}

	// eliminate null values in output
	public static String getNormalValue(String tmpValue) {
		if (tmpValue == null) {
			return "";
		} else {
			// tmpValue = tmpValue.replaceAll(",", "-");
			tmpValue = tmpValue.trim();
			return tmpValue;
		}
	}

	// return the current date value
	public static String getCurrentDateString(String formatString) {
		String retval = null;
		SimpleDateFormat formatter = new SimpleDateFormat(formatString);
		Date currDate = new Date();
		retval = formatter.format(currDate);
		return retval;
	}

	public static String getCurrentDateString() {
		String retval = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date currDate = new Date();
		retval = formatter.format(currDate);
		return retval;
	}

	public static org.bson.Document getDocumentFields(org.bson.Document doc, String[] fields) {
		org.bson.Document subdoc = new org.bson.Document();
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			Object value = doc.get(field);
			subdoc.append(field, value);
		}

		return subdoc;
	}

	public static String prepareFileKey(String key) {
		if (key == null) {
			return "NA";
		} else {

			key = key.trim();
			key = key.replace(":", "_");
			key = key.replace("-", "_");
			key = key.replaceAll("^\\.", "");
			key = key.replace("*", ".");
			key = key.replace("’", "");
			key = key.replace("?", "");
			key = key.replace("'", "");
			key = key.replace("\"", "");
			key = key.replace("\\", "_");
			key = key.replace("/", "_");
			key = key.replace("!", "");
			key = key.replace(",", "");
			key = key.replace("-", "");
			key = key.replace(" ", "_");
			key = key.replace("__", "_");
			return key;
		}
	}
	
	public static String cleanWord(String word) {
		// Use StringBuilder for efficient string manipulation
		StringBuilder cleanedWord = new StringBuilder();

		// Iterate over each character in the input word
		for (char ch : word.toCharArray()) {
			// Append to cleanedWord if the character is in the allowed set
			if (ALLOWED_CHARACTERS.indexOf(ch) != -1) {
				cleanedWord.append(ch);
			}
		}

		// Return the cleaned word as a string
		return cleanedWord.toString().trim();
	}
}
