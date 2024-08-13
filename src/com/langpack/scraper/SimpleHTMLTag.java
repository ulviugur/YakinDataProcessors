package com.langpack.scraper;

import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SimpleHTMLTag {
	public static final Logger logger = LogManager.getLogger("HTMLTag");

	private String source = null;

	private TreeMap<String, String> attributeMap = new TreeMap<>();
	private String content = "";

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	int cursor = 0;
	private String cursorString = null;

	public SimpleHTMLTag(String rawString) {
		this.setSource(rawString);
	}

	public SimpleHTMLTag(String rawString, boolean parse) {
		this.setSource(rawString);
		this.parse();
	}

	public String getLink() {
		// OLD WAY OF DOING IT; WE NOW USE JSOUP
		// return attributeMap.get("href");
		return getAttr("href");
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getAttr(String attrName) {
		Document doc = Jsoup.parse(this.source);
		Element body = doc.body();
		Attributes attList = body.childNode(0).attributes();
		String attrValue = attList.get(attrName);
		logger.info(attrValue);
		logger.info("");
		return attrValue;
	}

	public void parse() {
		String tagString = "";

		try {
			cursorString = source.substring(cursor, cursor + 1);
			if ("<".equals(cursorString)) {
				// logger.debug("starting tag .. ");
				tagString = readTag(cursorString);
			}
		} catch (NullPointerException e) {
			System.out.println("source of html : " + source);
		}

		if (">".equals(cursorString)) {
			tagString += cursorString;
			cursor++;
			cursorString = source.substring(cursor, cursor + 1);
			// logger.debug("closed tag, should continue with a value ..");
			while (!"<".equals(cursorString)) {
				// skip special characters if the source is read from a file which is wrapped
				if (!("\n".equals(cursorString) || "\r".equals(cursorString))) {
					content += cursorString;
				}
				cursor++;
				cursorString = source.substring(cursor, cursor + 1);
			}
		}

		// logger.debug("Finished parsing .. ");
	}

	private void readAttributes() {
		String attributeBuffer = "";
		String attribute = null;
		String valueBuffer = "";
		boolean inValue = false;
		while (!">".equals(cursorString)) {
			cursor++;
			cursorString = source.substring(cursor, cursor + 1);
			if ("\"".equals(cursorString) || "'".equals(cursorString)) {
				if (inValue) { // closing quotes of the value is reached
					attributeMap.put(attributeBuffer, valueBuffer);
					inValue = false;
					attributeBuffer = "";
					valueBuffer = "";
				} else {
					inValue = true;
				}
			} else if ("=".equals(cursorString)) {
				attribute = attributeBuffer;
			} else {
				if (!("\n".equals(cursorString) || "\r".equals(cursorString))) {
					if (inValue) {
						valueBuffer += cursorString;
					} else if (" ".equals(cursorString)) {
						// skip space just before attribute starts
					} else {
						attributeBuffer += cursorString;
					}
				}
			}

		}
	}

	private String readTag(String startLetter) {
		String tagBuffer = startLetter;
		// A tag can only end with a ">" (with no attributes) or with a " " (space) -
		// (with attributes)
		// in case of attributes, read attributes
		while (!">".equals(cursorString) && !" ".equals(cursorString)) {
			cursor++;
			cursorString = source.substring(cursor, cursor + 1);
			tagBuffer += cursorString;
		}
		if (" ".equals(cursorString)) {
			readAttributes();
		}
		return tagBuffer;
	}

	@Override
	public String toString() {
		String retval = "";
		retval += String.format("{ Source : %s\n, Content : %s\n", source, content);

		Set<String> departmentKeys = attributeMap.keySet();
		for (String attr : departmentKeys) {
			String value = attributeMap.get(attr);
			retval += String.format("[ %s , %s ]\n", attr, value);
		}
		retval += " }";
		return retval;
	}

	public static void main(String[] args) {
		// HTMLTag test = new HTMLTag("<a>Capital Federal</a>");
		SimpleHTMLTag test = new SimpleHTMLTag("<a itemprop='name' href='General_Donovan'>General Donovan</a>");
		test.parse();
		logger.info(test.toString());
	}
}
