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

public class ConfigReader {
	// ConfigReader cannot be if nested processes are using the same object. Changed
	// to be initialized ..
	private String cfgFileName = null;
	private File cfgFile = null;
	private Properties params = new Properties();
	private FileInputStream is = null;
	private final Charset cset = Charset.forName("UTF-8");

	public ConfigReader(String cfgFileName) {
		try {
			initialize(cfgFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getCfgFileName() {
		return cfgFile.getAbsolutePath();
	}

	public void initialize(String cfgFileName) throws IOException, FileNotFoundException {
		cfgFile = new File(cfgFileName);
		is = new FileInputStream(cfgFile);
		InputStreamReader isr = new InputStreamReader(is, cset);
		params.load(isr);
	}

	public String getValue(String tmpParam) {
		return params.getProperty(tmpParam);
	}

	public String getValue(String tmpParam, String defaultValue) {
		if (params.getProperty(tmpParam) != null) {
			return params.getProperty(tmpParam);
		} else {
			return defaultValue;
		}
	}

	public String getConfigInString() {
		String retval = "ConfigReader\n";
		retval += "-----------------------------\n";
		retval += "Config filename : " + cfgFileName + "\n";

		Set<Object> keys = params.keySet();
		for (Object key : keys) {
			Object value = params.get(key);
			retval += (String) key + "=" + (String) value + "\n";
		}
		retval += "-----------------------------\n";
		return retval;
	}

	public HashMap<String, String> getParameterMap() {
		HashMap<String, String> retval = new HashMap<>();
		Iterator<Object> iter = params.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String value = params.getProperty(key);
			retval.put(key, value);
		}
		return retval;
	}
}
