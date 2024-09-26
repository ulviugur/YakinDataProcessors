package com.langpack.dbprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;
import com.langpack.common.FileExporter;
import com.langpack.common.TextFileReader;

public class FilePipe {
	// Use this class to transfer data from one database to another database
	public static final Logger log4j = LogManager.getLogger("FilePipe");

	File inputFile = null;
	File outputFile = null;

	TextFileReader inputFileReader = null;
	FileExporter outputFileExporter = null;
	Method exportMethod = null;

	String inputDelimiter = null;
	String exportDelimiter = null;
	boolean skipEmptyLines = false;

	TreeSet<String> postcodeList = new TreeSet<>();
	ConfigReader cfg = null;

	public FilePipe(String configFileName) {
		cfg = new ConfigReader(configFileName);

		inputDelimiter = cfg.getValue("Input.FieldDelimiter");
		exportDelimiter = cfg.getValue("Export.FieldDelimiter");
		String skipParameter = cfg.getValue("Export.SkipEmptyLines");
		if (skipParameter != null && "Y".equalsIgnoreCase(skipParameter)) {
			skipEmptyLines = true;
		}

		String _inputFileName = cfg.getValue("Input.File");
		String _outputFileName = cfg.getValue("Export.File");
		File _inputFile = new File(_inputFileName);
		File _outputFile = new File(_outputFileName);

		setInputFile(_inputFile);
		setOutputFile(_outputFile);

	}

	public void init() {
		if (getInputFile() == null) {
			log4j.error(String.format("InputFile is not set !, Exitting !!"));
			System.exit(-1);
		}
		if (getOutputFile() == null) {
			log4j.error(String.format("OutputFile is not set !, Exitting !!"));
			System.exit(-1);
		}

		if (!getInputFile().exists()) {
			log4j.error(String.format("InputFile %s does not exist !, Exitting !!", getInputFile().getAbsolutePath()));
			System.exit(-1);
		}

		inputFileReader = new TextFileReader(getInputFile(), "UTF-8");
		try {
			outputFileExporter = new FileExporter(getOutputFile(), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (inputFileReader == null) {
			log4j.error(String.format("InputFile %s is not readable !, Exitting !!", getInputFile().getAbsolutePath()));
			System.exit(-1);
		}

		if (outputFileExporter == null) {
			log4j.error(
					String.format("OutputFile %s is not readable !, Exitting !!", getOutputFile().getAbsolutePath()));
			System.exit(-1);
		}
		log4j.info("Read and export files set and initialized ..");
	}

	public void process() {
		init();
		if (inputFileReader.openFile()) {
			String line = null;
			try {
				while ((line = inputFileReader.readLine()) != null) {
					try {
						String methodName = cfg.getValue("Export.Method");
						exportMethod = this.getClass().getMethod(methodName, String.class);
						String writeLine = (String) exportMethod.invoke(this, line);

						if (writeLine.equals("") && skipEmptyLines) {
							// skip this line
						} else {
							outputFileExporter.writeLineToFile(writeLine);
						}
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log4j.info("process completed ..");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			outputFileExporter.closeExportFile();
		} else {
			log4j.error(
					String.format("InputFile %s cannot be openned !, Exitting !!", getInputFile().getAbsolutePath()));
			System.exit(-1);
		}
	}

	public static void main(String[] args) {

		// TODO Auto-generated method stub

		FilePipe instance = new FilePipe(args[0]);
		instance.process();
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

}
