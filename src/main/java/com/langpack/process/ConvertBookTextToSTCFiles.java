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

import zemberek.tokenization.TurkishSentenceExtractor;

public class ConvertBookTextToSTCFiles {
	public static final Logger logger = LogManager.getLogger("ConvertBookTextToSTCFiles");

	File bookTextDir = null;
	File stcFilesDir = null;

	ConfigReader cfg;

	public ConvertBookTextToSTCFiles(String cfgFile) {

		cfg = new ConfigReader(cfgFile);

		String booksDirPath = cfg.getValue("booksdir");
		String stcDirPath = cfg.getValue("stcdir");

		bookTextDir = new File(booksDirPath);
		stcFilesDir = new File(stcDirPath);
	}


	private void exportSentences(String exportFileName, String text) {

		File exportFile = new File(stcFilesDir, exportFileName);
		FileExporter ex = null;
		try {
			ex = new FileExporter(exportFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String stcs = StringProcessor.extractSTCFromText(text);

		ex.writeLineToFile(stcs);

		logger.info("Finished with file {}", exportFile.getAbsolutePath());
	}

	public void runProcess() {
		File[] files = bookTextDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File item = files[i];
			logger.trace("[{}] - Processing file : {}", i, item);
			TextFileReader reader = new TextFileReader(item);
			String text = reader.readFile();
			String base = GlobalUtils.getFileBase(item);
			String exportFileName = base + ".stc";
			exportSentences(exportFileName, text);
		}
	}

	public static void main(String[] args) {

		ConvertBookTextToSTCFiles process = new ConvertBookTextToSTCFiles(args[0]);
		process.runProcess();
	}
}
