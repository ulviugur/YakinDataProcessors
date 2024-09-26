package com.langpack.common;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;

public class FileExtensionFilter implements FileFilter {
	// Support filtering of one or multiple extensions
	private HashSet<String> extensionList = new HashSet<>();

	public FileExtensionFilter(String tmpExt) {
		extensionList.add(tmpExt);
	}

	public FileExtensionFilter(String... tmpExtList) {
		if (tmpExtList != null && tmpExtList.length > 0) {
			for (String item : tmpExtList) {
				extensionList.add(item);
			}
		}
	}

	@Override
	public boolean accept(File pathname) {
		boolean retval = false;
		if (pathname.isFile()) {
			String[] fileParts = pathname.getName().split("\\.");
			if (fileParts.length >= 2) {
				String tmpExt = fileParts[fileParts.length - 1];
				if (extensionList.contains(tmpExt)) {
					retval = true;
				}
			}
		}
		return retval;
	}

}
