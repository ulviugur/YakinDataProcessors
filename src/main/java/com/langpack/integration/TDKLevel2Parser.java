package com.langpack.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.langpack.common.GlobalUtils;
import com.langpack.common.TextFileReader;
import com.langpack.scraper.TokenList;

public class TDKLevel2Parser extends Level2Parser {
	public static final Logger log4j = LogManager.getLogger("TDKLevel2Parser");
	String level2 = null;
	// TDKJson level2Object = null;
	public static HashSet<String> turSet = new HashSet<>();

	ArrayList<String> WORD_TYPES = new ArrayList<>();

	public TDKLevel2Parser() {
		setWordTypes();
	}

	@Override
	public void setLevel2(String level2) {
		this.level2 = level2;
		// level2Object = new TDKJson(level2);
	}

	@Override
	public String[] getWordsAsArray() {
		String[] retval = null;
		String splitString = null;
		if (level2 == null) {

		} else {
			splitString = TokenList.getTokenArrayForRegex();
			retval = level2.split(splitString);
		}
		return retval;
	}

	@Override
	public TreeSet<String> getWordsAsCollection() {
		TreeSet<String> retval = new TreeSet<>();
		Locale locale = new Locale("tr", "TR");
		String[] words = getWordsAsArray();
		for (String word2 : words) {
			String word = word2;
			if (TokenList.isToken(word)) {
				// don't add a token to the word list
			} else {
				word = word.trim();
				word = word.replaceAll("\\s+", "");
				word = word.toLowerCase(locale);
				retval.add(word);
			}
		}
		return retval;
	}

	@Override
	public String removeUnwantedKeywords(String inStr) {
		String retval = inStr;
		for (String key : KEYS) {
			retval = retval.replaceAll(key, " ");
		}
		return retval;
	}

	@Override
	public TreeSet<String> postProcess(TreeSet<String> inArray) {
		TreeSet<String> retval = new TreeSet<>();
		for (String phrase : inArray) {//
			String newPhrase = phrase;
			if (newPhrase.contains("_")) {
				newPhrase = phrase.replace("_", " ");
			}
			retval.add(newPhrase);
		}
		return retval;
	}

	@Override
	public String getLevel2Word() {
		String retval = null;
		if (level2 != null) {

		}
		return retval;
	}

	// public TDKJson getObject() {
	// return level2Object;
	// }

	public ArrayList<String> findWordTypes(String tmp) {
		ArrayList<String> retval = new ArrayList<>();
		for (int i = 0; i < WORD_TYPES.size(); i++) {
			String tmpType = WORD_TYPES.get(i);
			if (tmp.contains(tmpType)) {
				retval.add(tmpType);
			}
		}
		return retval;
	}

	public void setWordTypes() {
		WORD_TYPES.add("isim");
		WORD_TYPES.add("zamir");
		WORD_TYPES.add("fiil");
		WORD_TYPES.add("sıfat");
		WORD_TYPES.add("zarf");
	}

	public TDKWordWrapper parseJSONContent(String level2) {
		TDKWordWrapper retval = new TDKWordWrapper();

		String jsonStringRaw = level2.replaceAll("</body>", "").replaceAll("<body>", "").replaceAll("\n", "")
				.replace("'", "\'").trim();
		String jsonString1 = jsonStringRaw.replaceAll(":null", ":\"\""); // cleaning captured string
		String jsonString2 = "{word:" + jsonString1 + "}";

		JSONObject jsonMain = null;
		JSONArray propArray = null;

		String word = null;
		String langCode = null;
		String langContent = null;

		HashSet<String> meaningTypes = new HashSet<>();
		try {
			jsonMain = new JSONObject(jsonString2);

			JSONArray chapterArray = (JSONArray) jsonMain.get("word"); // sometimes multiple chapters are listed under
																		// the same word

			for (int chCount = 0; chCount < chapterArray.length(); chCount++) {
				JSONObject chItem = (JSONObject) chapterArray.get(chCount);

				word = (String) chItem.get("madde");
				TDKChapterItem chapterObject = new TDKChapterItem(word);
				chapterObject.setChapterId(Integer.toString(chCount + 1));
				chapterObject.setChapterName(word);
				try {
					String tmpLangCode = (String) chItem.get("lisan_kodu");
					langCode = tmpLangCode;
				} catch (Exception e) {
					// TODO: handle exception
				}

				try {
					String tmpLangContent = (String) chItem.get("lisan");
					langContent = tmpLangContent;
				} catch (Exception e) {
					// TODO: handle exception
				}

				try {
					String tmpCombinations = (String) chItem.get("birlesikler");
					if (!"".equals(tmpCombinations)) {
						String[] tmpSplitComb = tmpCombinations.split(",");
						TreeSet<String> uniqueCombs = GlobalUtils.getUniqueKeys(tmpSplitComb);
						for (String item : uniqueCombs) {
							retval.addCombination(Integer.toString(chCount + 1) + "-" + item.trim());
						}
					}

				} catch (Exception e) {
					// TODO: handle exception
				}

				try {
					JSONArray tmpProverbs = (JSONArray) chItem.get("atasozu");
					if (tmpProverbs.length() > 0) {
						for (int proCount = 0; proCount < tmpProverbs.length(); proCount++) {
							JSONObject tmp = (JSONObject) tmpProverbs.get(proCount);
							String proverb = (String) tmp.get("madde");
							retval.addProverb(Integer.toString(chCount + 1) + "-" + proverb.trim());
						}

					}

				} catch (Exception e) {
					// TODO: handle exception
				}

				JSONArray meaningsArray = (JSONArray) chItem.get("anlamlarListe");
				String dominantMeaningType = null; //
				// ArrayList<TDKMeaningItem> meaningArray = new ArrayList<TDKMeaningItem>();
				for (int meCount = 0; meCount < meaningsArray.length(); meCount++) {
					String sampleText = null;
					String authorText = null;

					JSONObject meaningItem = (JSONObject) meaningsArray.get(meCount);
					// log4j.info(">>> .. " + meaningItem);
					String meaning = (String) meaningItem.get("anlam");

					try {
						propArray = (JSONArray) meaningItem.get("ozelliklerListe");
						meaningTypes = new HashSet<>();
						boolean foundDominantType = false;
						for (int attCount = 0; attCount < propArray.length(); attCount++) {
							JSONObject props = (JSONObject) propArray.get(attCount);
							String tmpMeaningType = (String) props.get("tam_adi");
							try {
								String tmpAttType = (String) props.get("tur");
								turSet.add(tmpAttType + "-" + tmpMeaningType);

								if (tmpAttType.equals("3")) {
									dominantMeaningType = tmpMeaningType;
									foundDominantType = true;
								}
							} catch (JSONException ex) {
								log4j.info("Props list not found, same properties will continue");
							}

							meaningTypes.add(tmpMeaningType);
						}

						if (dominantMeaningType != null) {
							if (!foundDominantType && !dominantMeaningType.contains("#")) {
								dominantMeaningType = "#-" + dominantMeaningType;
							}
						}
					} catch (JSONException ex) {
						// log4j.info("Props list not found, same properties will continue");
					}
					try {
						// log4j.info(meaning + " : <> .... " +
						// GlobalUtils.convertArrayToString(meaningTypes));
						JSONArray sampleArray = (JSONArray) meaningItem.get("orneklerListe");
						JSONObject sample = (JSONObject) sampleArray.get(0);
						sampleText = sample.getString("ornek");

						JSONArray authorsArray = (JSONArray) sample.get("yazar");
						JSONObject author = (JSONObject) authorsArray.get(0);
						authorText = author.getString("tam_adi");

					} catch (JSONException ex) {

						// log4j.info("Props list not found, same properties will continue");
					}
					meaningTypes.add(dominantMeaningType);

					TDKMeaningItem tmp = new TDKMeaningItem(word, Integer.toString(chCount + 1),
							Integer.toString(meCount + 1), GlobalUtils.convertArrayToString(meaningTypes, ", "),
							meaning, sampleText, authorText);
					tmp.setLangCode(langCode);
					tmp.setLangContent(langContent);
					chapterObject.addMeaningItem(tmp);
					// meaningArray.add(tmp);
				}
				// chapterObject.setMeaningItems(meaningArray);
				retval.addChapterItem(chapterObject);
			}

		} catch (org.json.JSONException ex) {
			ex.printStackTrace();
		}

		return retval;
	}

	public TDKWordWrapper parseHTMLContent(String word, ChromeDriver driver) {
		TDKWordWrapper retval = new TDKWordWrapper();

		WebElement chapter = null;
		String chapterName = null;
		LinkedHashMap<String, String> chapterNameList = new LinkedHashMap<>();
		int countChapter = 0;

		try {
			while (true) {
				TDKChapterItem chItem = new TDKChapterItem(word);
				chItem.setWord(word);
				String chapterSectionName = String.format("bulunan-gts%s", countChapter);
				try {
					chapter = driver.findElement(By.id(chapterSectionName));
					chapterName = chapter.getText();
					chItem.setChapterId(Integer.toString(countChapter + 1));
					chItem.setChapterName(chapterName);
					// chapterNameList.put(Integer.toString(countChapter), chapterName);
				} catch (NoSuchElementException e) {
					break;
				}
				WebElement meaningsContainer = null;
				TDKMeaningItem meaningObject = null;
				try {
					WebElement atts = driver.findElement(By.id(String.format("ozellikler-gts%s", countChapter))); // DONT
																													// TOUCH
					WebElement typeItem = atts.findElement(By.tagName("i")); // DONT TOUCH
					if ("".equals(typeItem.getText())) {
						// if empty, we have multiple meanings, need to look into the meaningItems
						meaningsContainer = driver.findElement(By.id(String.format("anlamlar-gts%s", countChapter)));
						List<WebElement> meaningElements = meaningsContainer.findElements(By.tagName("p"));

						for (int meCount = 0; meCount < meaningElements.size(); meCount++) {
							try {
								meaningObject = new TDKMeaningItem();
								WebElement itemDetails = meaningElements.get(meCount);
								WebElement typeElement = itemDetails
										.findElement(By.cssSelector("i[style='color:orange']"));
								String itemType = typeElement.getText();
								meaningObject.setWordType(itemType);
								meaningObject.setMeaningId(Integer.toString(meCount + 1));
								chItem.addMeaningItem(meaningObject);
								// log4j.info(String.format("Chapter : %s, Meaning : %s, Type: %s",
								// countChapter, meCount, itemType));
							} catch (NoSuchElementException e) {
								break;
							}
						}

					} else {
						meaningObject = new TDKMeaningItem();
						meaningObject.setWordType(typeItem.getText());
						meaningObject.setMeaningId(Integer.toString(1));
						chItem.addMeaningItem(meaningObject);
						// log4j.info(String.format("Chapter : %s, Meaning : %s, Type: %s",
						// countChapter, 1, typeItem.getText()));
					}
				} catch (Exception ex) {
					break;
				}
				retval.addChapterItem(chItem);
				countChapter++;
			}

		} catch (Exception ex) {
			log4j.info(String.format("End of chapters for : %s", word));
		}
		return retval;
	}

	public static void main(String[] args) {
		log4j.info("Starting country data export ..");

		TextFileReader reader = new TextFileReader("S:\\Ulvi\\wordspace\\Wordlet\\data\\HTMLSample.html");
		// TODO Auto-generated method stub
		TDKLevel2Parser instance = new TDKLevel2Parser();

		log4j.info("Completed export process");
	}
}
