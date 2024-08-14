package com.langpack.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class FileExporter {

	FileOutputStream fos_export = null;
	BufferedOutputStream bout_export = null;

	File targetFile = null;
	String charset = null;
	Charset cset = Charset.forName("UTF-8");

	public FileExporter(String tmpFileName, String cset) throws FileNotFoundException {
		this(new File(tmpFileName), cset);
	}

	public FileExporter(String tmpFileName) throws FileNotFoundException {
		this(new File(tmpFileName), "UTF-8"); // use UTF-8 as the default char-set
	}

	public FileExporter(File tmpFile, String cs) throws FileNotFoundException {
		this(tmpFile, Charset.forName(cs));
	}

	public FileExporter(File tmpFile) throws FileNotFoundException {
		this(tmpFile, "UTF-8");
	}

	public FileExporter(File tmpFile, Charset cs) throws FileNotFoundException {
		targetFile = tmpFile;
		fos_export = new FileOutputStream(targetFile);
		bout_export = new BufferedOutputStream(fos_export);
		cset = cs;
	}

	public FileExporter(File tmpFile, Charset cs, boolean append) throws FileNotFoundException {
		targetFile = tmpFile;
		fos_export = new FileOutputStream(targetFile, append);
		bout_export = new BufferedOutputStream(fos_export);
		cset = cs;
	}

	public void writeLineToFile(String line) {
		try {
			if (charset == null) {
				bout_export.write(line.getBytes());
			} else {
				bout_export.write(line.getBytes(Charset.forName("UTF-8")));
			}

			bout_export.write("\r\n".getBytes());
			bout_export.flush();
			fos_export.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeStringToFile(String text) {
		try {
			if (charset == null) {
				bout_export.write(text.getBytes());
			} else {
				bout_export.write(text.getBytes(Charset.forName("UTF-8")));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void closeExportFile() {
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

	public File getExportFile() {
		return targetFile;
	}
}
