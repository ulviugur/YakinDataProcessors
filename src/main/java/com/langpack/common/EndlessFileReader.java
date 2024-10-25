package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import org.bson.Document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EndlessFileReader {

	public static final Logger log4j = LogManager.getLogger("EndlessFileReader");

	private File sourceFile = null;
	private TextFileReader reader = null;
	private boolean initialized;

	private int preLineCount = 0;
	private int postLineCount = 0;
	private int skipLinesCount = 0;
	
	private ArrayList<String> cache = new ArrayList<String>();// use a cache for returning blocks; will only analyse target stc
	private int CACHE_LENGTH;
	
	private Document fileProps = null; // this is relevant is the file is a book with properties
	
	public EndlessFileReader(File file) {
		if (file.exists()) {
			if (file.canRead()) {
				sourceFile = file;
				openFile();
			} else {
				log4j.warn("File {} cannot be read, cannot initialize the reader ..", file.getAbsolutePath());
			}
		} else {
			log4j.warn("File {} does not exist, cannot initialize the reader ..", file.getAbsolutePath());
		}
	}

	public EndlessFileReader(File file, Document propsDoc, int preLines, int postLines, int skipLines) {
		if (file.exists()) {
			if (file.canRead()) {
				sourceFile = file;
				fileProps = propsDoc;
				preLineCount = preLines;
				postLineCount = postLines;
				skipLinesCount = skipLines;
				CACHE_LENGTH = preLineCount + postLineCount + 1;
				openFile();
			} else {
				log4j.warn("File {} cannot be read, cannot initialize the reader ..", file.getAbsolutePath());
			}
		} else {
			log4j.warn("File {} does not exist, cannot initialize the reader ..", file.getAbsolutePath());
		}
	}

	private boolean openFile() {
		reader = new TextFileReader(sourceFile);
		
		// skip -n lines to strip out introductions to the book
		for (int i = 0; i < skipLinesCount; i++) {
			try {
				reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		initialized = true;
		return initialized;
	}

	public String readLine() {
		String retval = null;
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
		return retval;
	}

	public ArrayList<String> rollCache() {
		log4j.info("Reading from {}", sourceFile.getName());
		if (cache.size()>0) {
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

		EndlessFileReader rr = new EndlessFileReader(new File("D:\\BooksRepo\\process\\epub_stc\\71712_16.50_Treni_(Agatha_Christie).stc"), null, 1, 1, 10);
		if (rr.isInitialized()) {
			for (int i = 0; i < 3000; i++) {
				ArrayList<String> res = rr.rollCache();
				log4j.info(">>>> " + GlobalUtils.convertArraytoString(res));
			}
		} else {

		}
	}

}
