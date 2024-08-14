package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
		File targetDir = new File(targetDirPath);

		FileIterator sourceIter = null;

		try {
			sourceIter = new FileIterator(sourceDir);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Source directory cannot be initialized under {}", sourceDir.getAbsolutePath());
		}

//		FileIterator targetIter = null;
//		try {
//			targetIter = new FileIterator(targetDir);
//		} catch (InvalidRequestException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			logger.error("Target directory cannot be initialized under {}", targetDir.getAbsolutePath());
//			System.exit(-1);
//		}

		int totalCount = 0;

		File currSourceFile = sourceIter.getCurrFile();

		try {
			while (currSourceFile != null) {
				String ext = sourceIter.findFileExtension(currSourceFile);
				if (extensionsList.contains(ext)) {

					String baseFileName = sourceIter.findFileBase(currSourceFile);

					String exportFileName = targetDirPath + baseFileName + "." + "txt";
					File exportFile = new File(exportFileName);

					logger.info("[{}] Current source file: \"{}\"", totalCount, currSourceFile.getAbsolutePath());
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
}
