package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	// private static final String TEXT_DIR = "OEBPS/Text/"; // This structure is
	// not consistent, processing the whole lot is correct

	public String extractTextFromEpub(File epubFile) throws IOException {
		ZipFile zipFile = null;
		StringBuilder textContent = new StringBuilder();
		List<String> spineOrder = null;

		try {
			zipFile = new ZipFile(epubFile);

			spineOrder =getSpineOrderFromEpub(zipFile);

			// Process XHTML files in the order specified by the spine
			for (String spineEntry : spineOrder) {
				// First, try with "OEBPS/" prefix
				ZipEntry entry = zipFile.getEntry("OEBPS/" + spineEntry);

				// If entry is null, try without the "OEBPS/" prefix
				if (entry == null) {
					entry = zipFile.getEntry(spineEntry);
				}

				if (entry != null && (entry.getName().endsWith(".html") || entry.getName().endsWith(".xhtml")
						|| entry.getName().endsWith(".htm"))) {
					log4j.debug("Processing file : {}", entry.getName());
					try (InputStream inputStream = zipFile.getInputStream(entry)) {
						Document document = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");

						// Select all elements and append text from elements without children
						Elements elements = document.select("*");
						for (Element element : elements) {
							if (element.children().isEmpty()) {
								String elementText = element.text().trim();
								if (!elementText.isEmpty()) {
									textContent.append(elementText);
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
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
		return retval;
	}

	/**
     * Extracts the spine order from the EPUB's content.opf file.
     *
     * @param zipFile The ZipFile reference for the EPUB.
     * @return A list of spine entries (file paths) in reading order.
     * @throws IOException if an error occurs while reading the zip file.
     */
    public static List<String> getSpineOrderFromEpub(ZipFile zipFile) throws IOException {
        List<String> spineOrder = new ArrayList<>();
        ZipEntry contentOpfEntry = zipFile.getEntry("META-INF/container.xml");

        if (contentOpfEntry == null) {
            throw new IOException("META-INF/container.xml not found in the EPUB.");
        }

        // Locate content.opf from the container.xml
        try (InputStream containerStream = zipFile.getInputStream(contentOpfEntry)) {
            Document containerDoc = Jsoup.parse(containerStream, StandardCharsets.UTF_8.name(), "");
            String opfPath = containerDoc.select("rootfile").attr("full-path");

            // Read the content.opf file
            try (InputStream contentOpfStream = zipFile.getInputStream(zipFile.getEntry(opfPath))) {
                Document opfDocument = Jsoup.parse(contentOpfStream, StandardCharsets.UTF_8.name(), "");

                // Map item IDs to file paths from the manifest
                Map<String, String> idToHref = new HashMap<>();
                Elements items = opfDocument.select("manifest > item");
                for (Element item : items) {
                    String id = item.attr("id");
                    String href = item.attr("href");
                    idToHref.put(id, href);
                }

                // Build the spine order using idrefs and hrefs
                Elements spineItems = opfDocument.select("spine > itemref");
                for (Element itemref : spineItems) {
                    String idref = itemref.attr("idref");
                    if (idToHref.containsKey(idref)) {
                        spineOrder.add(idToHref.get(idref));
                    }
                }
            }
        }
        
        return spineOrder;
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
