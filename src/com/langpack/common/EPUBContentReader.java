package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EPUBContentReader {

    private static final String TEXT_DIR = "OEBPS/Text/";

    public String extractTextFromEpub(File epubFile) throws IOException {
        ZipFile zipFile = null;
        StringBuilder textContent = new StringBuilder();

        try {
            zipFile = new ZipFile(epubFile);

            for (ZipEntry entry : zipFile.stream().toList()) {
                if (entry.getName().startsWith(TEXT_DIR) && (entry.getName().endsWith(".html") || entry.getName().endsWith(".xhtml"))) {
                    InputStream inputStream = null;
                    try {
                        inputStream = zipFile.getInputStream(entry);
                        Document document = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");

                        Elements paragraphs = document.select("p");
                        for (Element paragraph : paragraphs) {
                            // Extract text from spans within each paragraph
                            Elements spans = paragraph.select("span");
                            for (Element span : spans) {
                                textContent.append(span.text()).append("");
                            }
                            // Append text of paragraph itself
                            textContent.append(paragraph.ownText()).append("\n");
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

        String retval = StringProcessor.cleanBookString(textContent.toString());
        return retval; // Trim to remove any trailing new lines
    }
}
