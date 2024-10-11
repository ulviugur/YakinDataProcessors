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
import com.langpack.common.FileLedger;
import com.langpack.common.GlobalUtils;
import com.langpack.common.InvalidRequestException;
import com.langpack.common.PDFContentReader;
import com.langpack.common.TextFileReader;

import zemberek.tokenization.TurkishSentenceExtractor;

public class ConvertBookTextToSentenceFiles {
	public static final Logger logger = LogManager.getLogger("ProcessBooksToSentenceFiles");

	File booksDir = null;
	File sentFilesDir = null;
	
	public ConvertBookTextToSentenceFiles(String booksDirPath, String sentFilesDirPath) {
		booksDir = new File(booksDirPath);
		sentFilesDir = new File(sentFilesDirPath);
	}
	
	private void exportSentences(String exportFileName, String text) {
        TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
        
        File exportFile = new File(sentFilesDir, exportFileName);
        FileExporter ex = null;
        try {
			ex = new FileExporter(exportFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        List<String> sentences = extractor.fromParagraph(text);
        for (int i = 0; i < sentences.size(); i++) {
        	String sent = sentences.get(i);
			logger.info("<{}> -> {}", i, sent);
			ex.writeLineToFile(sent);
		}
        logger.info("Finished with file {}", exportFile.getAbsolutePath());
        
	}
	public void runProcess() {
		File[] files = booksDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File item = files[i];
			logger.info("[{}] - Processing file : {}", i, item);
			TextFileReader reader = new TextFileReader(item);
			String text = reader.readFile();
			String base = GlobalUtils.getFileBase(item);
			String exportFileName = base + ".stc"; 
			exportSentences(exportFileName, text);
		}
	}
	public static void main(String[] args) {
		CommandLineArgs argsObject = new CommandLineArgs(args);
		String booksDirPath = argsObject.get("--booksdir");
		String sentFilesDirPath = argsObject.get("--sentfilesdir");
		ConvertBookTextToSentenceFiles process = new ConvertBookTextToSentenceFiles(booksDirPath, sentFilesDirPath);
		process.runProcess();
	}
}
