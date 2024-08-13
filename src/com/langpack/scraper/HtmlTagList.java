package com.langpack.scraper;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import com.langpack.common.TextFileReader;

public class HtmlTagList {
	TreeSet<String> tagList = new TreeSet<>();

	public void readTagsFromTextFile(String fileName) {
		File file = new File(fileName);
		readTagsFromTextFile(file);
	}

	public void readTagsFromTextFile(File file) {
		try {
			TextFileReader reader = new TextFileReader(file);

			String line = null;
			while ((line = reader.readLine()) != null) {
				tagList.add(line);
			}
			line = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean isHTMLTag(String tag) {
		if (tagList.contains(tag)) {
			return true;
		} else {
			return false;
		}
	}

	public TreeSet<String> removeHTMLTags(TreeSet<String> inList) {
		TreeSet<String> outList = new TreeSet<>();

		for (String token : inList) {
			if (!isHTMLTag(token)) {
				outList.add(token);
			}
		}
		return outList;
	}
}
