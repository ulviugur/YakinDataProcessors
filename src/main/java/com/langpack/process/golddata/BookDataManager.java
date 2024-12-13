package com.langpack.process.golddata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;
import com.langpack.common.LevensteinIndex;
import com.langpack.model.LibraryBookItem;
import com.langpack.model.LibraryLinkItem;
import com.langpack.scraper.ChromeHeadless;
import com.langpack.scraper.PageParser;
import com.langpack.scraper.Qualifier;

public class BookDataManager {

	public static final Logger log4j = LogManager.getLogger("CorrectAuthorNames");

	static PageParser parser = new PageParser();

	String sheetFolderStr = null;
	String sheetName = null;

	ConfigReader cfg = null;
	ConfigReader cfgChrome = null;

	ChromeHeadless myChrome = null;
	HashMap<String, String> authorsMap = new HashMap<String, String>();

	String binBaseURL = "https://1000kitap.com";

	public BookDataManager(String chromeConfigFile) {
		myChrome = new ChromeHeadless(chromeConfigFile);
	}

	public String validateAuthorOnDNR(String author) {
		author = author.replace(",", "");
		author = author.replace("(", "");
		author = author.replace(")", "");
		String page = null;
		String callURL = String.format("https://www.dr.com.tr/search?q=%s", author);
		log4j.info(callURL);
		try {
			page = GlobalUtils.callWebsite(callURL);

			if (page == null) {
				return null;
			}
			String qStringBlock = "js-facet-list-persons";
			int count = 0;
			while (true) {
				Qualifier authorQualifier = new Qualifier(qStringBlock, count, "<div", "/div>", Qualifier.GO_BACKWARD);
				String authorsDiv = parser.findBlock(page, authorQualifier);
				if (authorsDiv != null) {
					org.jsoup.nodes.Document doc = Jsoup.parse(authorsDiv);
					Elements authorSpans = doc.select("span.facet__checkbox-text");
					for (Element span : authorSpans) {
						String tmpAuthor = span.text();
						String result = checkAuthorName(author, tmpAuthor);
						if (result != null)
							return result;
					}
				} else {
					return null;
				}
				count++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String validateAuthorOn1000Kitap(String author) {
		if (author == null || "".equals(author)) {
			return null;
		}
		author = author.replace(",", "");
		author = author.replace("(", "");
		author = author.replace(")", "");
		String page = null;
		String authorStr = author.replace(" ", "+");
		String callURL = String.format("%s/ara?q=%s&bolum=yazarlar&hl=tr", binBaseURL, authorStr);

		try {
			page = myChrome.getURLContent(callURL);
			if (page == null) {
				return null;
			}
			org.jsoup.nodes.Document doc = Jsoup.parse(page);

			Elements authorSpans = doc.select("span.text.font-bold.truncate.text-16.w-full");
			for (Element span : authorSpans) {
				String tmpAuthor = span.text();
				String result = checkAuthorName(author, tmpAuthor);
				if (result != null)
					return result;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static String checkAuthorName(String _oriAuthor, String _foundAuthor) {
		String retval = null;

		String cleanOriAuthor = _oriAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");
		String cleanFoundAuthor = _foundAuthor.replace(".", " ").replace("-", " ").replace("  ", " ");

		// BOF - CASE1
		String oriAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanOriAuthor);
		String foundAuthor = GlobalUtils.normalizeUnicodeStringToUpper(cleanFoundAuthor);

		String[] partsOriAuthor = oriAuthor.split(" ");
		String[] partsFoundAuthor = foundAuthor.split(" ");

		Set<String> setOriAuthor = new TreeSet<>(Arrays.asList(partsOriAuthor));
		Set<String> setFoundAuthor = new TreeSet<>(Arrays.asList(partsFoundAuthor));

		int matchCount = matchingKeys(setOriAuthor, setFoundAuthor);

		if (matchCount >= 2) {
			retval = _foundAuthor;
			// EOF - CASE1
		} else {
			// BOF - CASE2
			int dist = LevensteinIndex.distance(GlobalUtils.convertArrayToString(setOriAuthor),
					GlobalUtils.convertArrayToString(setFoundAuthor));
			if (dist <= partsFoundAuthor.length) {
				retval = _foundAuthor;
			}
			// EOF - CASE2
		}

		return retval;
	}

	private static int matchingKeys(Set<String> original, Set<String> found) {
		// Count how many words from the original set are found in the found set
		int matchCount = 0;
		for (String word : original) {
			if (found.contains(word)) {
				matchCount++;
			}
		}

		// Calculate and return the percentage of words that matched
		return matchCount;
	}

	public LibraryLinkItem collectBookLinksFromBinBooks(String bookName, String author) {
		LibraryLinkItem retval = new LibraryLinkItem();
		ArrayList<String> links = new ArrayList<String>();
		String searchString =  String.format("%s %s", bookName, author).replaceAll("\\s+", "%20");
		String fullURL = String.format("%s/ara?q=%s&bolum=kitaplar", binBaseURL, searchString);
		log4j.info(fullURL);
		String page = null;
		try {
			page = myChrome.getURLContent(fullURL);
			if (page == null) {
				return retval;
			}
		} catch (Exception ex) {
			return retval;
		}

		String qString = "dr self-start max-w-full flex-row cursor";

		int jumper = 0;
		boolean foundLink = true;
		while (foundLink) {
			Qualifier pageQualifier = new Qualifier(qString, jumper, "<a", "</a>", Qualifier.GO_BACKWARD);
			String block = parser.findBlock(page, pageQualifier);
			if (block == null) {
				// log4j.error("Link is not found !");
				break;
			} else {
				org.jsoup.nodes.Document doc = Jsoup.parse(block);
				Element link = doc.selectFirst("a");

				if (link != null) {
					String subLink = link.attr("href");
					String fullLink = binBaseURL + subLink;
					links.add(fullLink);
					log4j.info("[{}] FullLink : {}", jumper, fullLink);
				}
			}
			jumper++;
		}
		LibraryLinkItem item = new LibraryLinkItem();
		item.setBookName(bookName);
		item.setAuthor(author);
		item.setScrapeURL(fullURL);
		item.setLinks(links);

		return item;
	}

	public LibraryBookItem scrapeBasicBookData(String scrapeURL, String bookName, String author) {
		LibraryBookItem retval = null;
		String page = null;
		try {
			page = myChrome.getURLContent(scrapeURL);
			if (page == null) {
				return retval;
			}
		} catch (Exception ex) {
			return retval;
		}

		String qStringButton = String.format("dr whk-15 overflow-hidden cursor");

		Qualifier buttonQualifier = new Qualifier(qStringButton, 0, "<button", "</button>", Qualifier.GO_BACKWARD);
		String button = parser.findBlock(page, buttonQualifier);
		if (button == null) {
			log4j.error("Image button is not found !");
			return retval;
		}

		org.jsoup.nodes.Document doc = Jsoup.parse(page);
		Element dataDiv = doc.selectFirst("div.dr.flex-row.flex-wrap.gap-1_5");
		Element divBookName = doc.selectFirst("h1.text.font-bold.truncate.text-20");

		if (dataDiv == null) {
			log4j.error("Data block not found at URL : {}!", scrapeURL);
			return retval;
		} else {
			Element divAuthor = dataDiv.selectFirst("span.text-alt.text-mavi.text-14.ml-1_5");

			String authorVerified = divAuthor.text();
			String bookNameVerified = divBookName.text();

			String[] partsBook = bookName.split(" ");
			int dist = LevensteinIndex.distance(bookNameVerified, bookName);
			if (dist <= partsBook.length) {
				retval = new LibraryBookItem();
				retval.setBookName(bookNameVerified);
				retval.setAuthor(authorVerified);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return retval;
	}


}
