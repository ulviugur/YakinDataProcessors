package com.langpack.common;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyList {
	public static final Logger log4j = LogManager.getLogger("FileLedger");

	// String Key and its Integer occurences
	TreeMap<String, Integer> _keyList = new TreeMap<>();

	public KeyList() {
		// TODO Auto-generated constructor stub
	}

	public void addKeyOccurence(String _key) {
		Integer existingOccurence = _keyList.get(_key);
		if (existingOccurence == null) {
			existingOccurence = 0;
		}
		existingOccurence++;
		_keyList.put(_key, existingOccurence);
	}

	public TreeMap<String, Integer> getMinOccurences(int benchmark) {
		TreeMap<String, Integer> retval = new TreeMap<>();
		log4j.info(String.format("Found %s records initally ..", _keyList.size()));
		int excluded = 0;
		for (String key : _keyList.keySet()) {
			Integer occurence = _keyList.get(key);
			if (occurence >= benchmark) {
				retval.put(key, occurence);
				log4j.info(String.format("Including %s due to %s occurences ! => [%s]", key, occurence, excluded));
			} else {
				excluded++;
				log4j.info(String.format("Excluding %s due to %s occurences ! => [%s]", key, occurence, excluded));
			}
		}
		log4j.info(
				String.format("Found %s records after elimination of < %s occurences .", _keyList.size(), benchmark));
		_keyList = retval;
		return _keyList;
	}

	public Iterator<String> getIterator() {
		Iterator<String> iter = _keyList.keySet().iterator();
		return iter;
	}

	public Set<String> getKeys() {
		return _keyList.keySet();
	}

	public int getSize() {
		return _keyList.size();
	}
}