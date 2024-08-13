package com.langpack.structure;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WordGenerator {
	static String[] LT = new String[] { " ", "a", "b", "c", "ç", "d", "e", "f", "g", "ğ", "h", "ı", "i", "j", "k", "l",
			"m", "n", "o", "ö", "p", "r", "s", "ş", "t", "u", "ü", "v", "y", "z" };
	// static String[] LT = new String[]{" ", "a", "b", "v", "y", "z"};
	static String[] _VOWELS = new String[] { "a", "e", "ı", "i", "o", "ö", "u", "ü" };
	static List<String> vowels = Arrays.asList(_VOWELS);
	static String[] _CONSONENTS = new String[] { "b", "c", "ç", "d", "f", "g", "ğ", "h", "j", "k", "l", "m", "n", "p",
			"r", "s", "ş", "t", "v", "y", "z" };
	static List<String> consonents = Arrays.asList(_CONSONENTS);

	// static String[] LT = new String[]{"a", "b", "c"};
	Integer[] currentWordArray = null;
	int CURSOR = -1;
//	int MAXLETTER = LTç;
	int WORD_LENGTH = 0;
	String startValue = null;
	String endValue = null;

	public static final Logger log4j = LogManager.getLogger("WordGenerator");

	public WordGenerator(int wordLength) {
		WORD_LENGTH = wordLength;
		currentWordArray = new Integer[WORD_LENGTH];
		CURSOR = currentWordArray.length - 1;
		for (int i = 0; i < WORD_LENGTH; i++) {
			currentWordArray[i] = 0;
		}
		// Use for testing purposes
		// PA = new int[] {0,0,0,0,0};
	}

	public void setCurrentValue(Integer[] currValue) {
		currentWordArray = currValue;
		CURSOR = currValue.length - 1;
	}

	// START VALUE IS EXCLUSIVE
	public void setStartValue(String startValue) {
		this.startValue = startValue;
		if (WORD_LENGTH != startValue.length()) {
			log4j.error(String.format(
					"WORD_LENGTH value [%s] cannot be different than target string's length[%s], skipping to set value !!",
					WORD_LENGTH, startValue.length()));
		} else {

			Integer[] currValue = new Integer[startValue.length()];
			for (int i = 0; i < startValue.length(); i++) {
				String letter = startValue.substring(i, i + 1);
				int val = -1;
				for (int j = 0; j < LT.length; j++) {
					if (LT[j].equals(letter)) {
						currValue[i] = j;
						break;
					}
				}
			}

			currentWordArray = currValue;
			CURSOR = currValue.length - 1;
		}
	}

	// END VALUE IS INCLUSIVE (not included)
	public void setEndValue(String endValue) {
		this.endValue = endValue;
	}

	public static String convertArrayToString(Integer[] tmp) {
		StringBuffer retval = new StringBuffer();
		for (Integer element : tmp) {
			String val = LT[element.intValue()];
			retval.append(val);
		}
		return retval.toString();
	}

	// if we want to run an array for a specific key [example:akı], we need an
	// initial point of that array. additionalDigits allow multiple digits to be
	// added (typically 1 will be used)
	public static Integer[] getFirstArrayOfKey(String key, int additionalDigits) {
		Integer[] retval = new Integer[key.length() + additionalDigits];
		int i = 0;
		for (; i < key.length(); i++) {
			String letter = key.substring(i, i + 1);
			int val = -1;
			for (int j = 0; j < LT.length; j++) {
				if (LT[j].equals(letter)) {
					retval[i] = j;
					break;
				}
			}
		}
		for (; i < retval.length; i++) {
			retval[i] = 0;
		}
		return retval;
	}

	public static Integer[] getLastArrayOfKey(String key, int additionalDigits) {
		Integer[] retval = new Integer[key.length() + additionalDigits];
		int i = 0;
		for (; i < key.length(); i++) {
			String letter = key.substring(i, i + 1);
			int val = -1;
			for (int j = 0; j < LT.length; j++) {
				if (LT[j].equals(letter)) {
					retval[i] = j;
					break;
				}
			}
		}
		for (; i < retval.length; i++) {
			retval[i] = LT.length - 1;
		}
		return retval;
	}

	public boolean checkMaxValue() {
		boolean retval = true;
		for (int i = 0; i < WORD_LENGTH; i++) {
			int val = currentWordArray[i];
			if (val != LT.length - 1) {
				retval = false;
				break;
			}
		}
		return retval;
	}

	public static int countWovels(String word) {
		int count = 0;
		List<String> vowels = Arrays.asList(_VOWELS);

		for (int i = 0; i < word.length(); i++) {
			String letter = word.substring(i, i + 1);
			if (vowels.contains(letter)) {
				count++;
			}
		}
		return count;
	}

	public static int countFirstTwoLetters(String word) {
		// if first two letters are both vowel or consonant, skip as invalid word
		int retval = 0;
		String letter1 = word.substring(0, 1);
		String letter2 = word.substring(1, 2);
		int inc1 = 0;
		int inc2 = 0;

		if (vowels.contains(letter1)) {
			inc1 = 1;
		} else {
			inc1 = -1;
		}
		if (vowels.contains(letter2)) {
			inc2 = 1;
		} else {
			inc2 = -1;
		}
		retval = inc1 + inc2;
		return retval;
	}

	public static int countMaxChain(String word) {
		int MAX_CHAIN = 0;
		int chainLength = 0;

		for (int i = 0; i < word.length(); i++) {
			String letter = word.substring(i, i + 1);
			int inc = 0;
			if (vowels.contains(letter)) {
				inc = 1;
			} else if (consonents.contains(letter)) {
				inc = -1;
			} else {
				inc = 0;
			}
			if (chainLength == 0) {
				chainLength = inc;
			} else if (chainLength * inc > 0) {
				chainLength = chainLength + inc;
			} else {
				chainLength = inc;
			}
			if (Math.abs(chainLength) > Math.abs(MAX_CHAIN)) {
				MAX_CHAIN = chainLength;
			}
		}
		return MAX_CHAIN;
	}

	@Override
	public String toString() {
		StringBuffer retval = new StringBuffer();
		if (currentWordArray == null) {
			return null;
		}

		for (Integer element : currentWordArray) {
			retval.append(LT[element]);
		}
		// log4j.info("PA >> " + GlobalUtils.convertArraytoString(PA));
		return retval.toString();
	}

	public String nextWord() {
		String word = null;
		boolean cont = true;
		do {
			String tmpValue = toString();
			if (endValue != null && endValue.equals(tmpValue)) {
				log4j.info(String.format("Reached the end value, end the generator ..", endValue));
				word = null;
				break;
			}
			inc();
			boolean check = checkMaxValue();
			if (check) {
				word = null;
				currentWordArray = null;
				break;
			}
			word = this.toString();
			// log4j.info("WordTest : " + word);
			int maxChain = countMaxChain(word);
			int firstLettersValue = countFirstTwoLetters(word);
			if (Math.abs(maxChain) > 2 || Math.abs(firstLettersValue) >= 2 || word.substring(0, 1).equals("ğ")) {
				// log4j.info("Skipping invalid value : " + word);
				continue;
			} else {
				break;
			}

		} while (cont);
		// log4j.info("NewWord : " + word);

		return word;
	}

	protected void inc() { // internal method
		int cursor = CURSOR;
		int curval;
		while (true) {
			curval = currentWordArray[cursor];
			curval++;
			if (curval == LT.length) {
				if (cursor == 0) {
					break;
				} else {
					currentWordArray[cursor] = 0;
					cursor--;
				}
			} else {
				currentWordArray[cursor] = curval;
				break;
			}
		}
		// log4j.info(String.format("Cursor: %s", curval));
	}

	public static void main(String[] args) {
		System.out.println("classpath=" + System.getProperty("java.class.path"));
		// int cc = WordGenerator.countMaxChain("zzz");

		WordGenerator instance = new WordGenerator(6);
		instance.setStartValue("akıl a");
		instance.setEndValue("akıl z");
		while (true) {
			instance.nextWord();
			String value = instance.toString();
			if (value == null) {
				log4j.info("Reached the end ..");
				break;
			} else {
				log4j.info("WORD : " + value);
			}

		}

	}
}
