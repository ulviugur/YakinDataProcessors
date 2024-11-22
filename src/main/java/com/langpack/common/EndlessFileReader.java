package com.langpack.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

public class EndlessFileReader {

	public static final Logger log4j = LogManager.getLogger("EndlessFileReader");

	private File sourceFile = null;
	private TextFileReader reader = null;
	private boolean initialized;

	private int preLineCount = 0;
	private int postLineCount = 0;
	private int skipPreLinesCount = 0;
	private int skipPostLinesCount = 0; // only applicable to fullRead option

	private ArrayList<String> cache = new ArrayList<String>();// use a cache for returning blocks; will only analyse
																// target stc
	private int CACHE_LENGTH;

	private Document fileProps = null; // this is relevant is the file is a book with properties

	private boolean fullRead;
	private ArrayList<String> stcArray = new ArrayList<String>();
	private int lineCount = 0;

	public EndlessFileReader(File file, Document propsDoc, int preLines, int postLines, int skipPreLines, int skipPostLines,
			boolean readAll) throws FileNotFoundException {
		if (file.exists()) {
			if (file.canRead()) {
				sourceFile = file;
				fileProps = propsDoc;
				preLineCount = preLines;
				postLineCount = postLines;
				skipPreLinesCount = skipPreLines;
				skipPostLinesCount = skipPostLines;
				CACHE_LENGTH = preLineCount + postLineCount + 1;
				fullRead = readAll; // read the whole file and read from memory directly
				openFile();
			} else {
				log4j.warn("File {} cannot be read, cannot initialize the reader ..", file.getAbsolutePath());
			}
		} else {
			log4j.warn("File {} does not exist, cannot initialize the reader ..", file.getAbsolutePath());
			throw new FileNotFoundException();
		}
	}

	private boolean openFile() {
		reader = new TextFileReader(sourceFile);
		initialized = true;
		// skip -n lines to strip out introductions to the book
		for (int i = 0; i < skipPreLinesCount; i++) {
			try {
				reader.readLine();
			} catch (IOException e) {
				initialized = false;
				e.printStackTrace();
			}
		}

		if (fullRead) {
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					stcArray.add(line);
				}
				if (stcArray.size() > skipPostLinesCount) {
					// removing last -n lines
					for (int i = 0; i < skipPostLinesCount; i++) {
						stcArray.remove(stcArray.size() - 1);
					}
				} else {
					initialized = false;
					return initialized;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return initialized;
	}

	public String readLine() {
		String retval = null;
		if (fullRead) {
			if (lineCount < stcArray.size() - 1) {
				lineCount++;
			} else {
				lineCount = 0;
				log4j.info("Reached the end of the file {}", sourceFile.getAbsolutePath());
			}
			try {
				retval = stcArray.get(lineCount);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {

			boolean trying = true;
			try {
				while (trying) {
					String tmp = reader.readLine();
					if (tmp == null) {
						log4j.info("End of file .. Going back to the beginning of {}", sourceFile.getAbsolutePath());
						reader.closeFile();
						openFile();
						cache = new ArrayList<String>(); // reset cache going into the new file
					} else {
						retval = tmp;
						trying = false;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return retval;
	}

	public ArrayList<String> rollCache() {
		// log4j.info("Reading from {}", sourceFile.getName());
		if (cache.size() > 0) {
			cache.remove(0);
		}
		while (cache.size() < CACHE_LENGTH) {
			String tmp = readLine();
			cache.add(tmp);
		}
		return cache;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public Document getFileProps() {
		return fileProps;
	}

	public static void main(String[] args) {

		EndlessFileReader rr = null;
		try {
			rr = new EndlessFileReader(new File("D:\\BooksRepo\\process\\epub_stc\\71712_16.50_Treni_(Agatha_Christie).stc"), null, 1, 1,
					10, 10, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (rr.isInitialized()) {
			for (int i = 0; i < 300000; i++) {
				ArrayList<String> res = rr.rollCache();
				log4j.info("{} >>>> {} ", i, GlobalUtils.convertArrayToString(res));
			}
		} else {

		}
	}

}
