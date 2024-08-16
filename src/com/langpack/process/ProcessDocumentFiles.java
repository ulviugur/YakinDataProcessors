package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.EPUBContentReader;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.PDFContentReader;

public class ProcessDocumentFiles {
	public static final Logger logger = LogManager.getLogger("ProcessPDFFiles");

	public static void main(String[] args) {

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String sourceDirPath = argsObject.get("--inputdir");
		// sourceDirPath = "C:\\BooksRepo\\SORTED\\A\\";
		// sourceDirPath = "C:\\BooksRepo\\SORTED\\tmp\\";

		String targetDirPath = argsObject.get("--outputdir");
		// targetDirPath = "C:\\BooksRepo\\Text\\A\\";

		String[] extensionsArray = argsObject.get("--extensions").split(" ");
		List<String> extensionsList = Arrays.asList(extensionsArray);
		

		File sourceDir = new File(sourceDirPath);

		FileIterator sourceIter = null;

		try {
			sourceIter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Source directory cannot be initialized under {}", sourceDir.getAbsolutePath());
		}

		int totalCount = 0;

		File currSourceFile = sourceIter.getCurrFile();

		try {
			while (currSourceFile != null) {
				logger.info("[{}] Current source file: \"{}\"", totalCount, currSourceFile.getAbsolutePath());

				String ext = sourceIter.findFileExtension(currSourceFile);
				if (extensionsList.contains(ext)) {

					String baseFileName = sourceIter.findFileBase(currSourceFile);
					String cleanBaseFileName = cleanBaseFilename(baseFileName);

					String exportFileName = targetDirPath + cleanBaseFileName + "." + "txt";
					File exportFile = new File(exportFileName);

					String content = null;

					if (currSourceFile.getAbsolutePath().toLowerCase().contains("pdf")) {
						PDFContentReader instance = new PDFContentReader(totalCount, currSourceFile);

						String rawContent = instance.readContent();
						if (rawContent == null || rawContent.length() < 100) {
							logger.warn("[{}] Skipping file: \"{}\" as it is empty or minimally readable !", totalCount,
									currSourceFile.getAbsolutePath());
							currSourceFile = sourceIter.moveToNextTarget();
							totalCount++;
						}
						content = instance.analyzeTextFollowingDashes(rawContent);

						if (content == null || content.length() < 100) {
							logger.warn("[{}] Skipping file: \"{}\" as it is empty or minimally readable !", totalCount,
									currSourceFile.getAbsolutePath());
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
							logger.info("Content of {} bytes are written to file {}", content.length(),
									exportFile.getAbsoluteFile());
						}

					} else if (currSourceFile.getAbsolutePath().toLowerCase().contains("epub")) {
						EPUBContentReader instance = new EPUBContentReader();
						try {
							content = instance.extractTextFromEpub(currSourceFile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (content == null || content.length() == 0) {
							logger.warn("[{}] Skipping file: \"{}\" as it is empty !", totalCount,
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
							logger.info("Content of {} bytes are written to file {}", content.length(),
									exportFile.getAbsoluteFile());
						}
					}
				} else {
					logger.info("[{}] File skipped due to untracked extension : \"{}\"", totalCount,
							currSourceFile.getAbsolutePath());
				}

				currSourceFile = sourceIter.moveToNextTarget();
				totalCount++;
			}
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exitting");
			System.exit(-1);
		}
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
            String authorPart1 = matcher.group(1).trim();  // (Orson Scott Card)
            String authorPart2 = matcher.group(2).trim();  // [Card, Orson Scott]

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
}
