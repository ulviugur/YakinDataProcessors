package com.langpack.scraper;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.GlobalUtils;

public class PageParser {
	public static final Logger log4j = LogManager.getLogger("PageParser");
	String content = null;
	String sourceURL = null;

	public PageParser() {

	}

	// set content of the parser when the data does not come from a file or webpage
	public void setPageContent(String contentString) {
		content = contentString;
	}

	public String getPageContent() {
		return content;
	}

	public static void main(String[] args) {
		PageParser parser = new PageParser();
		String result = GlobalUtils.callWebsite("file:///E://tmp//TEST_PC3.txt", 10000);

		Qualifier qualifier1 = new Qualifier("class='list2'", 1, "<table ", "</table>", Qualifier.GO_BACKWARD);
		String block1 = parser.findBlock(result, qualifier1);

		ArrayList<Integer> localistList = parser.findStringLocs(block1, "href='");

		for (int i = 0; i < localistList.size(); i++) {
			Qualifier qualifier2 = new Qualifier("href='", i, "<a ", "</a>", Qualifier.GO_BACKWARD);
			String tagBlock = parser.findBlock(block1, qualifier2);

			SimpleHTMLTag tag = new SimpleHTMLTag(tagBlock, true);
			System.out.println("tag:>>" + tag.getLink() + " ## " + tag.getContent());
		}

	}

	public String findBlock(String page, Qualifier blockQualifier) {
		if (page == null) {
			log4j.info("page is empty");
		}
		ArrayList<Integer> keyLocations = findStringLocs(page, blockQualifier.getQualifierString());
		if (keyLocations.size() == 0) {
			log4j.warn("Qualifier \"" + blockQualifier.getQualifierString() + " not found in page");
			// log4j.debug("Page : " + page);
			return null;
		}

		if (blockQualifier.getSkipQualifiers() >= keyLocations.size()) { // we came to the end of the qualifier list
			return null;
		}

		int keyLocation = keyLocations.get(blockQualifier.getSkipQualifiers()); // 1st key means 0 index
		// Find all locations with the block start location
		ArrayList<Integer> blockStarterLocations = findStringLocs(page, blockQualifier.getBlockStartQualifier());

		if (blockStarterLocations == null || blockStarterLocations.size() == 0) {
			log4j.warn(String.format("Block starter [%s] could not be found in the page !!",
					blockQualifier.getBlockStartQualifier()));
		}

		int blockStarterLoc = -1;
		int blockEndLoc = -1;
		for (int i = 0; i < blockStarterLocations.size(); i++) { // walk through all block starters
			Integer tmp = blockStarterLocations.get(i);
			if (blockQualifier.getSearchDirection() == Qualifier.GO_FORWARD) {
				if (tmp > keyLocation) {
					blockStarterLoc = tmp;
					break;
				}
			} else if (blockQualifier.getSearchDirection() == Qualifier.GO_BACKWARD) {
				if (tmp > keyLocation) {
					blockStarterLoc = blockStarterLocations.get(i - 1);
					break;
				}
			}

			if (i + 1 == blockStarterLocations.size()) { // check if you have reached the end of the list
				if ((keyLocation > tmp.intValue() && blockQualifier.getSearchDirection() == Qualifier.GO_BACKWARD) || (keyLocation < tmp.intValue() && blockQualifier.getSearchDirection() == Qualifier.GO_FORWARD)) {
					blockStarterLoc = tmp.intValue();
					break; // if we reached the end of the list and we can find the next block in forward
							// direction, take it
				}
			}
		}

		ArrayList<Integer> blockEndLocations = findStringLocs(page, blockQualifier.getBlockEndQualifier());
		if (blockEndLocations == null || blockEndLocations.size() == 0) {
			log4j.warn(String.format("Block starter [%s] could not be found in the page !!",
					blockQualifier.getBlockEndQualifier()));
		}

		// finding the first endBlock after the startBlock string
		for (Integer tmp : blockEndLocations) {
			if (tmp.intValue() > blockStarterLoc) {
				blockEndLoc = tmp;
				break;
			}
		}

		String blockString = null;
		try {
			blockString = page.substring(blockStarterLoc, blockEndLoc + blockQualifier.getBlockEndQualifier().length());
		} catch (StringIndexOutOfBoundsException ex) {
			log4j.debug("############ Error in search for Qualifier : " + blockQualifier + " within " + page
					+ "###########");
		}
		return blockString;
	}

	public ArrayList<Integer> findStringLocs(String _tmpPage, String _keyword) {
		int cursor = 0;
		ArrayList<Integer> _keyLocations = new ArrayList<>();
		int tmpLoc = 1;
		while ((tmpLoc = _tmpPage.indexOf(_keyword, cursor)) > -1) {
			_keyLocations.add(new Integer(tmpLoc));
			cursor = tmpLoc + 1;
		}
		return _keyLocations;
	}

	public ArrayList<String> parseLists(String parseSpan, String listStarter, String listTerminator,
			String listQualifier) {
		ArrayList<String> arrayItems = new ArrayList<>();

		ArrayList<Integer> startsetLocs = findStringLocs(parseSpan, listStarter);
		ArrayList<Integer> endsetLocs = findStringLocs(parseSpan, listTerminator);

		Iterator<Integer> startIter = startsetLocs.iterator();
		Iterator<Integer> endIter = endsetLocs.iterator();

		while (startIter.hasNext()) {
			int tmpStart = startIter.next();
			int tmpEnd = endIter.next();
			String listItem = parseSpan.substring(tmpStart + listStarter.length(), tmpEnd);
			if ((listQualifier == null) || (listItem.indexOf(listQualifier) > -1)) {
				arrayItems.add(listItem);
			}
		}

		return arrayItems;
	}

}
