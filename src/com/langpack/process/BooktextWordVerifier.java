package com.langpack.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.CommandLineArgs;
import com.langpack.common.FileExporter;
import com.langpack.common.FileIterator;
import com.langpack.common.GlobalUtils;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.TextFileReader;
import com.yakin.morphtr.AnalysisWrapper;
import com.yakin.morphtr.WordModel;
import com.yakin.morphtr.WordType;

// Doesn't work through a database but over a file
// Imports raw text into a file as they come. 
// !! Does not create a unique list 
public class BooktextWordVerifier {
	public static final Logger logger = LogManager.getLogger("BooktextWordVerifier");

	File sourceDir = null;
	String targetDBConn = null;

	File currFile = null;

	// Using a file as database source and use a HashMap to prevent duplicates
	File dbInFile = null;
	TreeSet<String> dbContent = new TreeSet<>();

	FileIterator dbFileIter = null;

	ArrayList<String> skippedList = new ArrayList<String>();

	TextFileReader txtInFileReader = null;
	FileExporter dbExporter = null;

	AnalysisWrapper analysis = new AnalysisWrapper();

	public BooktextWordVerifier(String dbFileStr) {

		dbInFile = new File(dbFileStr);

		try {
			dbFileIter = new FileIterator(dbInFile);
		} catch (InvalidRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void runProcess() {
		File currSourceFile = dbFileIter.getCurrFile();

		String fileBase = GlobalUtils.getFileBase(currSourceFile);

		TextFileReader reader = new TextFileReader(currSourceFile);
		boolean opened = reader.openFile();
		if (opened) {

			int insertCount = 0;
			int lineCount = 0;
			String word = null;

			AnalysisWrapper aw = new AnalysisWrapper();
			
			Set<String> stemSet = new TreeSet<String>();
			Set<String> exactMatchSet = new TreeSet<String>();
			Set<String> possibleMatchSet = new TreeSet<String>();
			Set<String> noMatchSet = new TreeSet<String>();

			try {
				while ((word = reader.readLine()) != null) {
					if ("".equals(word.trim())) {
						continue;
					}
					lineCount++;
					if (lineCount % 1000 == 0) {
						logger.info("[{}] Word : {}", lineCount, word);						
					} 

					List<WordModel> results = aw.getWordAnalysis(word);
					for (int i = 0; i < results.size(); i++) {
						WordModel res = results.get(i);
						WordType wt = res.getType();
						
						switch (wt) {
						case EXACT_MATCH:
							String exactWord = res.getOriginalWord();
							String root = res.getRootWord();
							exactMatchSet.add(exactWord);
							stemSet.add(root);
							break;
						case POSSIBLE_MATCH:
							String oriWord = res.getOriginalWord();
							String matchWord = res.getMappedWord();
							possibleMatchSet.add(oriWord + "\t" + matchWord);
							stemSet.add(res.getStem());
							break;
						case UNRECOGNIZED_WORD:
							noMatchSet.add(word);
							break;
						default:
							logger.info("Unexpected case ..");
							break;
							
						}
					}
				} //  end of while
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // end of try
			
			String fileStemsStr = fileBase + "_STEMS.txt";
			String fileExactMatchStr = fileBase + "_EXACT_MATCH.txt";
			String filePossibleMatchStr = fileBase + "_POSSIBLE_MATCH.txt";
			String fileNoMatchStr = fileBase + "_NO_MATCH.txt";

			File fileStems = new File(currSourceFile.getParent(), fileStemsStr);
			File fileExactMatch = new File(currSourceFile.getParent(), fileExactMatchStr);
			File filePossibleMatch = new File(currSourceFile.getParent(), filePossibleMatchStr);
			File fileNoMatch = new File(currSourceFile.getParent(), fileNoMatchStr);
			
			try {
				FileExporter expStems = new FileExporter(fileStems);
				String stemStr = GlobalUtils.convertSettoStringLines(stemSet, null);
				expStems.writeStringToFile(stemStr);
				expStems.closeExportFile();
				
				FileExporter expExactMatch = new FileExporter(fileExactMatch);
				String exactMatchStr = GlobalUtils.convertSettoStringLines(exactMatchSet, null);
				expExactMatch.writeStringToFile(exactMatchStr);
				expExactMatch.closeExportFile();
				
				FileExporter expPossibleMatch = new FileExporter(filePossibleMatch);
				String possibleMatchStr = GlobalUtils.convertSettoStringLines(possibleMatchSet, null);
				expPossibleMatch.writeStringToFile(possibleMatchStr);
				expPossibleMatch.closeExportFile();
				
				FileExporter expNoMatch = new FileExporter(fileNoMatch);
				String noMatchStr = GlobalUtils.convertSettoStringLines(noMatchSet, null);
				expNoMatch.writeStringToFile(noMatchStr);
				expNoMatch.closeExportFile();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			

		} else {
			logger.info("[{}] Source file {} could not be opened ! Skipping.", currSourceFile.getAbsolutePath());
			skippedList.add(currSourceFile.getAbsolutePath());
		}
		/*
		 * // write to the database to the new dbfile try {
		 * 
		 * File nextDBFile = dbFileIter.getNextFile();
		 * logger.info("Next dbfile : {}, writing \"{}\" records in total.",
		 * nextDBFile.getAbsolutePath(), dbContent.size()); dbExporter = new
		 * FileExporter(nextDBFile); Iterator<String> iterDict = dbContent.iterator();
		 * while (iterDict.hasNext()) { String word = iterDict.next();
		 * dbExporter.writeLineToFile(word); } dbExporter.closeExportFile();
		 * logger.info("New file  {} written, export completed",
		 * nextDBFile.getAbsolutePath()); } catch (FileNotFoundException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		logger.info("Process completed");
		logger.info("");
		
	}

	public static void main(String[] args) {
		
		AnalysisWrapper aw = new AnalysisWrapper();
		List<WordModel> res = aw.getWordAnalysis("zerrediyo");

		CommandLineArgs argsObject = new CommandLineArgs(args);

		String targetDBFilePath = argsObject.get("--dbinfile");

		BooktextWordVerifier process = new BooktextWordVerifier(targetDBFilePath);

		process.runProcess();

	}
}
