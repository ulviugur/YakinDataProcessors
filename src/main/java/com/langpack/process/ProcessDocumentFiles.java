package com.langpack.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.ConfigReader;
import com.langpack.common.EPUBContentReader;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.FileLedger;
import com.langpack.common.GlobalUtils;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.PDFContentReader;
import com.langpack.common.StringProcessor;
import com.langpack.common.TextFileReader;

public class ProcessDocumentFiles {
	public static final Logger log4j = LogManager.getLogger("ProcessDocumentFiles");

	String sourceDirPath = null;
	String targetDirPath = null;
	String extensionStr = null;
	String idFilePath = null;
	
	String[] extensionsArray = null;
	List<String> extensionsList = null;
	String ledgerFilePath = null;
	FileLedger ledger = null;
	ConfigReader cfg = null;

	public ProcessDocumentFiles(String cfgReaderPath) {

		cfg = new ConfigReader(cfgReaderPath);

		sourceDirPath = cfg.getValue("inputdir");
		targetDirPath = cfg.getValue("outputdir");
		extensionStr = cfg.getValue("extensions");

		extensionsArray = extensionStr.split(" ");
		extensionsList = Arrays.asList(extensionsArray);

		ledgerFilePath = cfg.getValue("ledgerfile");
		idFilePath = cfg.getValue("idfile");
		
		if (ledgerFilePath == null || "".equals(ledgerFilePath.trim())) {
			log4j.info("No ledger parameter set, exitting ..");
			System.exit(-1);
		}

		try {
			ledger = new FileLedger(ledgerFilePath);
			ledger.loadLedger();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log4j.error("Ledgerfile cannot be opened : {}, exitting", ledgerFilePath);
			System.exit(-1);
		}

	}

	public void process() {
		File sourceDir = new File(sourceDirPath);

		if (sourceDir.exists()) {
			File[] dirs = sourceDir.listFiles();

			for (int i = 0; i < dirs.length; i++) {
				File dir = dirs[i];
				if (!dir.isDirectory()) {
					log4j.warn("{} is not a directory, expect a directory to process, skipping ..",
							dir.getAbsolutePath());
				} else {
					processDirectory(dir, targetDirPath, extensionsList);
				}
			}
		} else {
			log4j.info("Inputdir {} does not exist", sourceDir.getAbsolutePath());
		}
	}

	public void processDirectory(File sourceDir, String targetDirPath, List<String> extensionsList) {
		FileIterator sourceIter = null;
		try {
			sourceIter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log4j.error("Source directory cannot be initialized under {}", sourceDir.getAbsolutePath());
		}

		int totalCount = 0;
		File currSourceFile = sourceIter.getCurrFile();
		try {
			while (currSourceFile != null) {
				log4j.info("[{}] Current source file: \"{}\"", totalCount, currSourceFile.getAbsolutePath());

				String ext = GlobalUtils.getFileExtension(currSourceFile);
				if (extensionsList.contains(ext)) {

					if (ledger.isInledger(currSourceFile.getAbsolutePath())) {
						log4j.info("The file exists in ledger, skipping {} ..", currSourceFile.getAbsolutePath());
					} else {

						String baseFileName = GlobalUtils.getFileBase(currSourceFile);
						String cleanBaseFileName = cleanBaseFilename(baseFileName);

						String exportFileName = targetDirPath + cleanBaseFileName + "." + "txt";
						File exportFile = new File(exportFileName);

						String content = null;

						if (currSourceFile.getAbsolutePath().toLowerCase().contains("pdf")) {
							PDFContentReader instance = new PDFContentReader(totalCount, currSourceFile);

							String rawContent = instance.readContent();
							if (rawContent == null || rawContent.length() < 100) {
								log4j.warn("[{}] Skipping file: \"{}\" as it is empty or minimally readable !",
										totalCount, currSourceFile.getAbsolutePath());
								currSourceFile = sourceIter.moveToNextTarget();
								totalCount++;
							}

							content = instance.analyzeTextFollowingDashes(rawContent);

							if (content == null || content.length() < 100) {
								log4j.warn("[{}] Skipping file: \"{}\" as it is empty or minimally readable !",
										totalCount, currSourceFile.getAbsolutePath());
								currSourceFile = sourceIter.moveToNextTarget();
								totalCount++;
							} else {

								FileExporter fExporter = null;
								try {
									fExporter = new FileExporter(exportFile);
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								fExporter.writeStringToFile(content);
								fExporter.closeExportFile();
								log4j.info("Content of {} bytes are written to file {}", content.length(),
										exportFile.getAbsoluteFile());
							}

						} else if (currSourceFile.getAbsolutePath().toLowerCase().contains("epub")) {

							content = getEPUBContent(currSourceFile);
							if (content == null || content.length() == 0) {
								log4j.warn("[{}] Skipping file: \"{}\" as it is empty !", totalCount,
										currSourceFile.getAbsolutePath());
							} else {
								FileExporter fExporter = null;
								try {
									fExporter = new FileExporter(exportFile);
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								fExporter.writeStringToFile(content);
								fExporter.closeExportFile();
								log4j.info("Content of {} bytes are written to file {}", content.length(),
										exportFile.getAbsoluteFile());
							}
						}
					}
				} else {
					log4j.debug("[{}] File skipped due to untracked extension : \"{}\"", totalCount,
							currSourceFile.getAbsolutePath());
				}
				ledger.addtoLedger(currSourceFile.getAbsolutePath());

				currSourceFile = sourceIter.moveToNextTarget();
				totalCount++;
			}
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log4j.error("Exitting");
			System.exit(-1);
		} catch (Exception ex) {
			ledger.closeLedger();
		}
	}

	private String getEPUBContent(File currSourceFile) {
		String retval = null;
		EPUBContentReader instance = new EPUBContentReader();
		try {
			retval = instance.extractTextFromEpub(currSourceFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.print(retval);
		return retval;
	}

	public static String cleanBaseFilename(String tmpFilename) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tmpFilename.length(); i++) {
			String letter = tmpFilename.substring(i, i + 1);
			if (letter.equals("\u2552")) {
				sb.append("ı");
			} else {
				sb.append(letter);
			}
		}

		String updatedStr = sb.toString();
		Pattern pattern = Pattern.compile("\\(([^\\(\\)]+) \\[(.+)\\]\\)");

		String newFileName = updatedStr;
		Matcher matcher = pattern.matcher(updatedStr);
		if (matcher.find()) {
			// Extract the author's name parts from the match
			String authorPart1 = matcher.group(1).trim(); // (Orson Scott Card)
			String authorPart2 = matcher.group(2).trim(); // [Card, Orson Scott]

			boolean keywordsMatching = areKeywordsMatching(authorPart1, authorPart2);
			// Check if the two parts are the same but reversed
			if (keywordsMatching) {
				// Remove the second occurrence (inside the square brackets)
				newFileName = matcher.replaceAll("($1)");
			}
		}
		String strippedFileName = newFileName.replace("(z-lib.org)", "");
		String strippedFileName2 = strippedFileName.replaceAll("\\s+$", "");
		String strippedFileName3 = strippedFileName2.replaceAll("\\s+", "_");

		return strippedFileName3;
	}

	public void addPrefixtoFiles() {
		File idFile = new File(idFilePath);
		TextFileReader reader = new TextFileReader(idFile);
		boolean opened = reader.openFile();
		String line = null;
		TreeMap<String, String> idMap = new TreeMap<String, String>();
		int maxid = 0;
		try {
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				String id = tokens[1];
				int intid = Integer.parseInt(id);
				maxid = Math.max(maxid, intid);
				String filename = tokens[2];
				idMap.put(filename, id);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info("Loaded {} ids for files", idMap.size());

		File targetdir = new File(targetDirPath);

		File[] fileList = targetdir.listFiles();
		Integer nextid = maxid + 1;
		int count = 0;
		for (int i = 0; i < fileList.length; i++) {
			log4j.info("index : " + i);
			File currFile = fileList[i];
			String filename = currFile.getName();

			Pattern pattern = Pattern.compile("\\d{5}");
			Matcher matcher = pattern.matcher(filename);

			// Find all matches
			boolean res = matcher.find();
			if (res) {
				String currid = matcher.group();
			} else {
				String newid = idMap.get(filename);
				String newFilename = null;
				if (newid == null) {
					newid = nextid.toString();
					nextid++;
				} 
				
				newFilename = newid + "_" + filename;
				log4j.info("File {}", newFilename);
				
				File newFile = new File(currFile.getParentFile(), newFilename);
				
				log4j.info("Currfile : {}, NewFile : {}", currFile, newFile);
				currFile.renameTo(newFile);
				
				count++;
			}

		}		
	}

	private static boolean areKeywordsMatching(String part1, String part2) {
		// Split the names into individual words by spaces or commas
		String[] name1Parts = part1.split("\\s*,\\s*|\\s+");
		String[] name2Parts = part2.split("\\s*,\\s*|\\s+");

		// Sort the arrays to ensure the words can be compared regardless of order
		Arrays.sort(name1Parts);
		Arrays.sort(name2Parts);

		// Check if the sorted arrays are equal
		return Arrays.equals(name1Parts, name2Parts);
	}

	public static void main(String[] args) {
		ProcessDocumentFiles instance = new ProcessDocumentFiles(args[0]);

		instance.process();
		//instance.addPrefixtoFiles(); // if the files do not have a d{5} prefix. Should be added to the end of the process to rename txt files.
	}

}
