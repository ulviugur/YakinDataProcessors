package com.langpack.scraper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Properties;

import com.langpack.common.FileExporter;
import com.langpack.common.TextFileReader;

public class TurkishCharMapper {
	private static String CSET_DEFAULT = "UTF-8";
	private static Charset cset = Charset.forName(CSET_DEFAULT);
	private Properties prop = new Properties();

	public TurkishCharMapper(String mappingFileName) {
		this(new File(mappingFileName), cset);
	}

	public TurkishCharMapper(File mappingFile, Charset cset) {

		InputStream is = null;
		try {
			is = new FileInputStream(mappingFile);
		} catch (FileNotFoundException ex) {
			System.out.println("File not found : " + mappingFile.getAbsolutePath());
		}
		try {
			prop.load(new InputStreamReader(is, Charset.forName("UTF-8")));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public String replaceCodedCharacters(String inString) {
		String retval = inString;

		Iterator<Object> iter = prop.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String value = prop.getProperty(key);
			String tmp = retval.replaceAll(key, value);
			if (tmp.equals(retval)) {

			} else {
				System.out.print("");
				retval = tmp;
			}
		}

		return retval;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		TextFileReader map1 = new TextFileReader("S:\\Ulvi\\wordspace\\Wordlet\\config\\TürkceKarakterler.txt");
		String line = null;
		try {
			line = map1.readLine();
			System.out.print(line);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		TurkishCharMapper mapper = new TurkishCharMapper(args[0]);
		TextFileReader reader = new TextFileReader("S:\\Ulvi\\wordspace\\Wordlet\\data\\sample.txt");
		String text = reader.readFile();
		String text2 = mapper.replaceCodedCharacters(text);

		try {
			FileExporter exporter = new FileExporter("S:\\Ulvi\\wordspace\\Wordlet\\data\\export.txt");
			exporter.writeLineToFile(text2);
			exporter.closeExportFile();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(text2);
		System.out.println();
	}
}
