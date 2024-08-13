package com.langpack.common;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileLedger {
	public static final Logger log4j = LogManager.getLogger("FileLedger");

	FileOutputStream fos_export = null;
	BufferedOutputStream bout_export = null;
	Reader fis_import = null;
	BufferedReader bis_import = null;

	HashSet<String> _ledger = new HashSet<>();

	File targetFile = null;
	Charset charset = null;
	static Charset DEFAULT_CSET = Charset.forName("UTF-8");

	public FileLedger(String tmpFileName) throws FileNotFoundException {
		this(new File(tmpFileName));
	}

	public FileLedger(File tmpFile) throws FileNotFoundException {
		this(tmpFile, DEFAULT_CSET);
	}

	public FileLedger(File tmpFile, Charset cs) throws FileNotFoundException {
		targetFile = tmpFile;
		charset = cs;
	}

	public boolean isInledger(String searchKey) {
		if (_ledger.contains(searchKey)) {
			return true;
		} else {
			return false;
		}
	}

	// if the key already in the ledger. it is denied to add it again
	public String addtoLedger(String addKey) {
		if (!isInledger(addKey)) {
			_ledger.add(addKey);
			writeToLedger(addKey);
		} else {
			addKey = null;
		}

		return addKey;
	}

	public boolean loadLedger() {
		boolean retval = true;
		try {
			targetFile.createNewFile();
			fis_import = new FileReader(targetFile);
			bis_import = new BufferedReader(fis_import);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			log4j.error("Error opening ledger file, exitting !");
			System.exit(-1);
		}
		String line = null;
		try {
			int count = 0;
			while ((line = bis_import.readLine()) != null) {
				_ledger.add(line);
				count++;
			}
			bis_import.close();
			fis_import.close();
			retval = true;
			log4j.info(String.format("Ledger file content [%s items] loaded succesfully !", count));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			fos_export = new FileOutputStream(targetFile, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		bout_export = new BufferedOutputStream(fos_export);
		return retval;
	}

	protected void writeToLedger(String line) {
		try {
			bout_export.write(line.getBytes());
			bout_export.write("\r\n".getBytes());
			bout_export.flush();
			fos_export.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeLedger() {
		try {
			bout_export.flush();
			bout_export.close();
			fos_export.flush();
			fos_export.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			FileLedger ledger = new FileLedger("S:\\Ulvi\\wordspace\\Wordlet\\data\\TDKLedger_05.ledger");
			ledger.loadLedger();
			String again = ledger.addtoLedger("@@@");
			boolean ret1 = ledger.isInledger("kkk");
			boolean ret2 = ledger.isInledger("@@@");
			log4j.info("again = " + again);
			log4j.info("ret1 = " + ret1);
			log4j.info("ret2 = " + ret2);
			ledger.closeLedger();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
