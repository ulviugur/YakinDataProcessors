package com.langpack.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ConfigReader {
	private File cfgFile;
	private Properties params = new Properties();
	private final Charset cset = Charset.forName("UTF-8");

	// Constructor initializes the configuration file
	public ConfigReader(String cfgFileName) {
		initialize(cfgFileName);
	}

	// Returns the absolute path of the config file
	public String getCfgFileName() {
		return cfgFile.getAbsolutePath();
	}

	// Loads the properties from the configuration file
	public void initialize(String cfgFileName) {
		cfgFile = new File(cfgFileName);

		// Ensure proper resource management with try-with-resources
		FileInputStream is;
		try {
			is = new FileInputStream(cfgFile);
			InputStreamReader isr = new InputStreamReader(is, cset);

			// Load the properties from the file
			params.load(isr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	// Get the value of a specific parameter
	public String getValue(String tmpParam) {
		return params.getProperty(tmpParam);
	}

	// Get the value of a specific parameter with a default value
	public String getValue(String tmpParam, String defaultValue) {
		return params.getProperty(tmpParam, defaultValue);
	}

	// Get a string representation of the configuration file
	public String getConfigInString() {
		StringBuilder retval = new StringBuilder("ConfigReader\n");
		retval.append("-----------------------------\n");
		retval.append("Config filename : ").append(cfgFile.getAbsolutePath()).append("\n");

		for (Map.Entry<Object, Object> entry : params.entrySet()) {
			retval.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		retval.append("-----------------------------\n");
		return retval.toString();
	}

	// Convert parameters to a HashMap
	public HashMap<String, String> getParameterMap() {
		HashMap<String, String> retval = new HashMap<>();
		for (Map.Entry<Object, Object> entry : params.entrySet()) {
			retval.put((String) entry.getKey(), (String) entry.getValue());
		}
		return retval;
	}
}
