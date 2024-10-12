package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EPUBContentReader {

	public static final Logger log4j = LogManager.getLogger("ProcessPDFFiles");

	//private static final String TEXT_DIR = "OEBPS/Text/"; // This structure is not consistent, processing the whole lot is correct

	public String extractTextFromEpub(File epubFile) throws IOException {
		ZipFile zipFile = null;
		StringBuilder textContent = new StringBuilder();

		try {
			zipFile = new ZipFile(epubFile);

			List<? extends ZipEntry> entries = zipFile.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList();

			for (ZipEntry entry : entries) {
				String name = entry.getName();

				if ((entry.getName().endsWith(".html") || entry.getName().endsWith(".xhtml")
						|| entry.getName().endsWith(".htm"))) {
					log4j.info("Processing file : {}", name);
					InputStream inputStream = null;
					try {
						inputStream = zipFile.getInputStream(entry);
						Document document = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");

						// Select all elements
						Elements elements = document.select("*");

						// Iterate through each selected element
						for (Element element : elements) {
							// Only append text from elements that do not have any child elements
							if (element.children().isEmpty()) {
								String elementText = element.text().trim(); // Get the text of the deepest elements

								// Append text content if it's not empty
								if (!elementText.isEmpty()) {
									// textContent.append(elementText).append("\n"); \n was added for separation
									// yet some books even separate words within spans
									// which splits words with space ("Eğer cinayet işleme mişseniz")
									// Lets test it without space.
									textContent.append(elementText);
								}
							}
						}

					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e; // Re-throw exception to signal failure
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// log4j.info("Dirty text : {}", textContent.toString());
		String retval = StringProcessor.cleanBookString(textContent.toString());
		// log4j.info("Clean text : {}", retval);
		return retval; // Return the cleaned-up string
	}

	private List<Integer> monitorKey(String content, String searchKey) {
		List<Integer> retval = new ArrayList<>();

		// Edge case: if searchKey or content is null or empty, return empty list
		if (content == null || searchKey == null || content.isEmpty() || searchKey.isEmpty()) {
			return retval;
		}

		// Start searching from index 0
		int index = content.indexOf(searchKey);

		// Keep searching until no more occurrences are found
		while (index != -1) {
			retval.add(index); // Add the index to the list
			index = content.indexOf(searchKey, index + 1); // Continue searching from the next character
		}

		return retval; // Return list of indices where searchKey appears
	}

}
