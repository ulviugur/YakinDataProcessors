package com.langpack.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import com.langpack.common.ConfigReader;
import com.langpack.common.GlobalUtils;

public class SyllableBreaker {
	/*
	 * Rule1 : Türkçede kelime içinde iki ünlü arasındaki ünsüz, kendinden sonraki
	 * ünlüyle hece kurar: a-ra-ba, bi-çi-mi-ne, in-sa-nın, ka-ra-ca vb. Rule2 :
	 * Kelime içinde yan yana gelen iki ünsüzden ilki kendinden önceki ünlüyle,
	 * ikincisi kendinden sonraki ünlüyle hece kurar: al-dı, bir-lik, sev-mek vb.
	 * Rule3 : Kelime içinde yan yana gelen üç ünsüz harften ilk ikisi kendinden
	 * önceki ünlüyle, üçüncüsü kendinden sonraki ünlüyle hece kurar: alt-lık,
	 * Türk-çe, kork-mak vb. Rule4 : Batı kökenli kelimeler, Türkçenin hece yapısına
	 * göre hecelere ayrılır: band-rol, kont-rol, port-re, prog-ram, sant-ral,
	 * sürp-riz, tund-ra, volf-ram vb.
	 */
	ConfigReader cfgObject = null;
	public static String[] VOWELS_TURKISH = new String[] { "a", "ı", "o", "u", "e", "i", "ö", "ü" };
	public static String[] CONSONENTS_TURKISH = new String[] { "b", "c", "ç", "d", "f", "g", "ğ", "h", "j", "k", "l",
			"m", "n", "p", "r", "s", "ş", "t", "v", "y", "z" };
	Locale TURKISH_LOCALE = new Locale("tr", "TR");

	public static String WORD_SEPARATOR = "_";
	public static String SYL_SEPARATOR = ".";

	public SyllableBreaker(String cfgFileName) {
		cfgObject = new ConfigReader(cfgFileName);
	}

	private static ArrayList<String> breakUp(String _word) {
		ArrayList<String> retval = new ArrayList<>();
		String word = _word.toLowerCase();
		String maxCoCo = getMaxConsequentConsonants(word);
		int vowelCount = getVowelCount(word);
		int loc = -1;
		if (maxCoCo == null) {
			retval.add(word);
			return retval;
		} else {
			loc = word.indexOf(maxCoCo);
		}

		if (maxCoCo.length() == 3) { // apply Rule3
			String part1 = word.substring(0, loc + 2);
			String part2 = word.substring(loc + 2);
			ArrayList<String> partsOf1 = breakUp(part1);
			ArrayList<String> partsOf2 = breakUp(part2);
			retval.addAll(partsOf1);
			retval.addAll(partsOf2);
		} else if (maxCoCo.length() == 2) { // Apply Rule2
			String part1 = word.substring(0, loc + 1);
			String part2 = word.substring(loc + 1);
			ArrayList<String> partsOf1 = breakUp(part1);
			ArrayList<String> partsOf2 = breakUp(part2);
			retval.addAll(partsOf1);
			retval.addAll(partsOf2);
			return retval;
		} else if (maxCoCo.length() == 1) { // Apply Rule1
			if (vowelCount > 1) {
				ArrayList<Integer> locs = getVowelLocations(word);
				String part1 = null;
				String part2 = null;
				if (locs.get(0) == 0) { // first letter is a vowel, it must be the first syllable
					part1 = word.substring(0, 1);
					part2 = word.substring(1);
				} else {
					part1 = word.substring(0, locs.get(0) + 1);
					part2 = word.substring(locs.get(0) + 1);
				}
				ArrayList<String> partsOf1 = breakUp(part1);
				ArrayList<String> partsOf2 = breakUp(part2);
				retval.addAll(partsOf1);
				retval.addAll(partsOf2);
				return retval;
			} else {
				retval.add(word);
				return retval;
			}
		} else if (maxCoCo.length() == 0) { // Single vowel
			retval.add(word);
		}
		return retval;
	}

	public static ArrayList<String> revertCase(String word, ArrayList<String> tmpArray) {
		// hece hesaplamasi kücük harfler üzerinden yapiliyor
		// orjinal haline geri döndürmemiz gerekiyor
		int loc = 0;
		ArrayList<String> retval = new ArrayList<>();
		for (int i = 0; i < tmpArray.size(); i++) {
			String tmpWord = tmpArray.get(i);
			String correctedWord = "";
			for (int j = 0; j < tmpWord.length(); j++) {
				String letter = word.substring(loc, loc + 1);
				correctedWord += letter;
				loc++;
			}
			retval.add(correctedWord);

		}
		return retval;
	}

	public static ArrayList<String> break2Syls(String word) {
		String[] word_array = word.split("\\s");
		ArrayList<String> retval = new ArrayList<>();
		for (int i = 0; i < word_array.length; i++) {
			String item = word_array[i];
			ArrayList<String> pass1 = breakUp(item);
			ArrayList<String> pass2 = revertCase(item, pass1);
			retval.addAll(pass2);

			if (i < word_array.length - 1) {
				retval.add(WORD_SEPARATOR);
			}
		}
		return retval;
	}

	public static String convertSylArrayToString(Collection<String> array, String separator) {
		StringBuilder sb = new StringBuilder();
		if (array != null) {
			Iterator<String> iter = array.iterator();
			while (iter.hasNext()) {
				String atom = iter.next();
				if (WORD_SEPARATOR.equals(atom)) {
					int tmp = sb.lastIndexOf(SYL_SEPARATOR);
					sb.deleteCharAt(tmp);
					sb.append(" ");
				} else {
					sb.append(atom);
					sb.append(separator);
				}
			}
		}
		String retval = sb.toString();
		if (retval.length() > 0) {
			retval = retval.substring(0, retval.length() - 1);
		}
		return retval;
	}

	public static int getVowelCount(String word) {
		int vowelCount = 0;
		for (int i = 0; i < word.length(); i++) {
			String letter = word.substring(i, i + 1);
			boolean vowel = Helper.checkInArray(letter, VOWELS_TURKISH);
			if (vowel) {
				vowelCount++;
			}
		}
		return vowelCount;
	}

	public static ArrayList<Integer> getVowelLocations(String word) {
		ArrayList<Integer> vowelLocations = new ArrayList<>();
		for (int i = 0; i < word.length(); i++) {
			String letter = word.substring(i, i + 1);
			boolean vowel = Helper.checkInArray(letter, VOWELS_TURKISH);
			if (vowel) {
				vowelLocations.add(i);
			}
		}
		return vowelLocations;
	}

	public static String getMaxConsequentConsonants(String word) { // start from startLoc to jump to a later consonant
																	// than the first one
		String retval = null; // maximum number of consecutive consonants
		String currentValue = null; // current number of consecutive consonants
		for (int i = 0; i < word.length(); i++) {
			String letter = word.substring(i, i + 1);
			// System.out.println("Letter = " + letter);
			boolean consonant = Helper.checkInArray(letter, CONSONENTS_TURKISH);
			// System.out.println("Consonant = " + consonant);
			if (consonant) {
				if (currentValue == null) {
					currentValue = letter;
				} else {
					currentValue = currentValue + letter;
				}
			} else {
				// hit a vowel
				if (currentValue != null) {
					if (retval != null) {
						if (currentValue.length() > retval.length()) {
							retval = currentValue; // save if maximum
						}
					} else {
						retval = currentValue; // save if none was found before
					}
				}
				currentValue = null; // reset cursor value
			}
		}
		return retval;
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));

		SyllableBreaker instance = new SyllableBreaker(args[0]);
		// instance.scanTablesforWords();
		// instance.saveWordListingDB();
		// String value = SyllableBreaker.getMaxConsequentConsonants("adımlarından");
		ArrayList<String> value = SyllableBreaker.break2Syls("Abaza peyniri");
		System.out.println("Value = " + GlobalUtils.convertArrayToString(value, " | "));
		// instance.breakUp("akil");
	}
}
