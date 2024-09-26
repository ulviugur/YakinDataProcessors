package com.langpack.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextFileReader {
	File readFile = null;
	FileInputStream fis = null;
	InputStreamReader isr = null;
	BufferedReader reader = null;
	private static String CSET_DEFAULT = "UTF-8";

	private Charset cset = null;

	public static final Logger log4j = LogManager.getLogger("TextFileReader");

	public TextFileReader(String tmpFileName) {
		this(new File(tmpFileName), CSET_DEFAULT);
	}

	public TextFileReader(File tmpFile) {
		this(tmpFile, CSET_DEFAULT);
	}

	public TextFileReader(File tmpFile, String _cset) {
		readFile = tmpFile;
		cset = Charset.forName(_cset);
		openFile();
	}

	public boolean openFile() {
		boolean retval = false;
		if (reader == null) {
			try {
				fis = new FileInputStream(readFile);
				isr = new InputStreamReader(fis, cset);
				reader = new BufferedReader(isr);
				retval = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (reader != null) {
			retval = true;
		}
		return retval;
	}

	public String readLine() throws IOException {
		if (reader == null) {
			openFile();
		}
		String line = reader.readLine();
		if (line == null) {
			closeFile();
		}
		return line;
	}

	public String readFile() {
		StringBuffer retval = new StringBuffer();
		String line = null;
		try {
			while ((line = readLine()) != null) {
				retval.append(line);
				retval.append("\r\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval.toString();
	}

	public void closeFile() {
		try {
			reader.close();
			isr.close();
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getFileName() {
		String retval = readFile.getAbsolutePath();
		return retval;
	}

	public static void main(String[] args) {
		log4j.info("Starting loading file ..");

		// TODO Auto-generated method stub
		TextFileReader instance = new TextFileReader(
				"S:/Ulvi/workspace/GeoSystems/data/Locality/JuneDelivery/Final/ALB.txt");
		try {
			String line = instance.readLine();
			System.out.println("Line : " + line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log4j.info("Finishing process");
	}
}
