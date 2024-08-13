package com.langpack.scraper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.langpack.common.BasicClass;
import com.langpack.common.GlobalUtils;
import com.langpack.dbprocess.XLDBInterface_1;

public class TurengLookup extends BasicClass {

	XLDBInterface_1 xlInterface = null;
	String fullURL = null;
	Boolean importData = false;
	Boolean pageFound = false;
	PageParser parser = null;
	String sourceFolder = null;

	public TurengLookup(String cfgFileName) {
		super(cfgFileName);

		sourceFolder = cfg.getValue("SourceFolder");
		importData = Boolean.parseBoolean(cfg.getValue("ImportData"));
		parser = new PageParser();

		// TODO Auto-generated constructor stub
	}

	public void scrapeContent() {

		if (importData) {
			xlInterface = new XLDBInterface_1(cfg.getCfgFileName());
			xlInterface.process();
		}

		try {
			ResultSet res = psSelect1.executeQuery();

			while (res.next()) {

				String word = null;
				// String wordType = null;

				try {
					word = res.getString(1);
					word = word.trim();
					// wordType = res.getString(2);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				fullURL = String.format("%s%s", sourceFolder, word);

				String page = GlobalUtils.callWebsite(fullURL);
				if (page == null || page.contains("Maybe the correct one is")) {
					page = "";
					pageFound = false;
				} else {
					pageFound = true;
				}

				String blockKey = "<h2>Meanings of <b>\"" + word + "\"</b> in English Turkish Dictionary";

				if (pageFound) {
					Qualifier pageQualifier = new Qualifier(blockKey, 0, "<table", "</table>", Qualifier.GO_FORWARD);
					parser.setPageContent(page);
					// log4j.info(page);

					String block = parser.findBlock(page, pageQualifier);
					if (block == null) {
						log4j.error("Content table not found !");
					} else {
						log4j.info("+++++ Content table in " + fullURL);
						insertItemsFromBlock(block);
						// End of page, jump to the next page
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param block
	 */
	private void insertItemsFromBlock(String block) {
//		log4j.info(block);
		SimpleHTMLTag tag = new SimpleHTMLTag(block, true);

		int count = 0;
		PageParser parser2 = new PageParser();
		while (true) {
			parser2.setPageContent(tag.getContent());

			Qualifier categoryQualifier = new Qualifier("class=\"hidden-xs\"", count, "<td", "</td>",
					Qualifier.GO_BACKWARD);

			String categoryBlock = parser2.findBlock(tag.getSource(), categoryQualifier);
			SimpleHTMLTag categoryTag = new SimpleHTMLTag(categoryBlock, true);

			if (categoryBlock == null) {
				log4j.info("Came to the end of the list @ item " + count);
				break;
			}

			Qualifier idQualifier = new Qualifier("class=\"rc0 hidden-xs\"", count, "<td", "</td>",
					Qualifier.GO_BACKWARD);
			String idBlock = parser2.findBlock(tag.getSource(), idQualifier);
			SimpleHTMLTag idTag = new SimpleHTMLTag(idBlock, true);

			Qualifier engWordQualifier = new Qualifier("class=\"en tm\"", count, "<a", "</a>", Qualifier.GO_FORWARD);
			String engWordBlock = parser2.findBlock(tag.getSource(), engWordQualifier);
			SimpleHTMLTag engWordTag = new SimpleHTMLTag(engWordBlock, true);

			Qualifier trWordQualifier = new Qualifier("class=\"tr ts\"", count, "<a", "</a>", Qualifier.GO_FORWARD);
			String trWordBlock = parser2.findBlock(tag.getSource(), trWordQualifier);
			SimpleHTMLTag trWordTag = new SimpleHTMLTag(trWordBlock, true);

			Qualifier typeQualifier = new Qualifier("class=\"en tm\"", count, "<i>", "</i>", Qualifier.GO_FORWARD);
			String typeBlock = parser2.findBlock(tag.getSource(), typeQualifier);
			SimpleHTMLTag typeTag = new SimpleHTMLTag(typeBlock, true);

			String recordStr = String.format("[%s, %s, %s, %s]", engWordTag.getContent(), trWordTag.getContent(),
					typeTag.getContent(), categoryTag.getContent());
			try {
				psInsert1.setString(1, categoryTag.getContent());
				psInsert1.setString(2, engWordTag.getContent());
				psInsert1.setString(3, trWordTag.getContent());
				psInsert1.setString(4, typeTag.getContent());
				psInsert1.setString(5, fullURL);
				psInsert1.setString(6, idTag.getContent());
				psInsert1.executeUpdate();
				log4j.info("Inserted record : " + recordStr);
			} catch (java.sql.SQLIntegrityConstraintViolationException e) {
				// TODO Auto-generated catch block
				// log4j.info("Unique constraint failure : " + recordStr);
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			count++;
		}
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		TurengLookup instance = new TurengLookup(args[0]);
		instance.scrapeContent();

		// instance.scanTablesforWords();
		// instance.saveWordListingDB();

	}

}
