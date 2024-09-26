package com.langpack.scraper;

import static org.jsoup.Jsoup.parse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TestApp {
	private final static String URL_TO_PARSE = "https://b-ok.cc/book/5517559/8ae8bf";

	public static void main(String[] args) throws IOException {
		// these two lines are only required if your Internet
		// connection uses a proxy server
		// System.setProperty("http.proxyHost", "my.proxy.server");
		// System.setProperty("http.proxyPort", "8081");
		String LINK = "NO_IDEA";
		URL url = new URL(URL_TO_PARSE);
		Document doc = parse(url, 30000);

		Elements links = doc.select("a[href$=" + LINK + "]");
		int linksSize = links.size();
		if (linksSize > 0) {
			if (linksSize > 1) {
				System.out.println("Warning: more than one link found.  Downloading first match.");
			}
			Element link = links.first();
			String linkUrl = link.attr("abs:href");

			byte[] bytes = Jsoup.connect(linkUrl).header("Accept-Encoding", "gzip, deflate")
					.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
					.referrer(URL_TO_PARSE).ignoreContentType(true).maxBodySize(0).timeout(600000).execute()
					.bodyAsBytes();

			try {

				String savedFileName = link.text();
				if (!savedFileName.endsWith(".mp3")) {
					savedFileName.concat(".mp3");
				}
				FileOutputStream fos = new FileOutputStream(savedFileName);
				fos.write(bytes);
				fos.close();

				System.out.println("File has been downloaded.");
			} catch (IOException e) {
				System.err.println("Could not read the file at '" + linkUrl + "'.");
			}
		} else {
			System.out.println("Could not find the link ending with '" + LINK + "' in web page.");
		}
	}

}