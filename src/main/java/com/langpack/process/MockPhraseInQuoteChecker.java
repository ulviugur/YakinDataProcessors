package com.langpack.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.GlobalUtils;
import com.langpack.common.TextFileReader;
import com.langpack.model.AnalysisWrapper;
import com.langpack.model.suffix.SuffixModel;
import com.langpack.model.suffix.SuffixModelTemplate;
import com.langpack.model.suffix.SuffixType;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;

public class MockPhraseInQuoteChecker {

	public static final Logger log4j = LogManager.getLogger("MockPhraseInQuoteChecker");

	public static Set<String> LEMMAS = new TreeSet<String>();

	public static TreeSet<String> secposs = new TreeSet<String>();
	public static TreeSet<String> priposs = new TreeSet<String>();

	public static TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
	public static WordGenerator gen = morphology.getWordGenerator();

	public static TreeSet<String> getDerivedWords(String word) {
		TreeSet<String> derivedWords = new TreeSet<String>();

		WordAnalysis results = morphology.analyze(word);
		List<Morpheme> morphs = null;
		List<MorphemeData> morphData = null;
		List<SingleAnalysis> altStructures = results.getAnalysisResults();
		// log4j.info("Found {} analysis for word {}", altStructures.size(), word);
		DictionaryItem rootItem = null;

		int count = 0;
		for (Iterator<SingleAnalysis> iterator = altStructures.iterator(); iterator.hasNext();) {

			SingleAnalysis singleAnalysis = (SingleAnalysis) iterator.next();
			log4j.info("Analiz {} : {}", count, singleAnalysis.toString());
			rootItem = singleAnalysis.getDictionaryItem();
			derivedWords.add(rootItem.lemma);
			derivedWords.add(rootItem.root);
			// log4j.info("Lemma for {} is {}", word, root.lemma);
			morphs = singleAnalysis.getMorphemes();
		    morphData = singleAnalysis.getMorphemeDataList();
		    
		    List<Morpheme> filteredMorphs = getDerivationalMorphs(morphs, word);

			for (int i = 0; i < filteredMorphs.size(); i++) {

				List<Morpheme> subMorphs = filteredMorphs.subList(0, i + 1);
				List<Result> res = gen.generate(rootItem, subMorphs);

				for (Iterator<Result> iterator2 = res.iterator(); iterator2.hasNext();) {
					Result result = iterator2.next();

					derivedWords.add(result.surface);
					//log4j.info(result.surface);
					SingleAnalysis deeper = result.analysis;

					DictionaryItem deeperItem = deeper.getDictionaryItem();
					derivedWords.add(deeperItem.lemma);
				}
			}
			count++;
		}
		//log4j.info("Derived list {} : {}", word, GlobalUtils.convertArrayToString(derivedWords));
		return derivedWords;
	}
	
	// Go through all Morphemes and only select derivational ones to collect possible derivations of the root word
	public static List<Morpheme> getDerivationalMorphs(List<Morpheme> morphsList, String word) {
		List<Morpheme> retval = new ArrayList<Morpheme>();
		
		for (int i = 0; i < morphsList.size(); i++) {
			Morpheme item = morphsList.get(i);
			if (item.derivational) {
				retval.add(item);
			}
		}
		return retval;	
	}

	public static void main(String[] args) {

		TreeSet<String> wordList1 = getDerivedWords("aldanmamalı");

//		AnalysisWrapper analysisWrapper = new AnalysisWrapper();
//
//		String word = "adaletsizliğiyle";
//
//		List<WordModel> wordModels = analysisWrapper.getWordAnalysis(word);
//
//		for (WordModel wordModel : wordModels) {
//			log4j.info("Original Word: " + wordModel.getOriginalWord());
//			log4j.info("Mapped Word: " + wordModel.getMappedWord());
//			log4j.info("Stem: " + wordModel.getStem());
//			log4j.info("Root Word: " + wordModel.getRootWord());
//			log4j.info("Suffix Type: " + wordModel.getSuffixType());
//			log4j.info("Match Type: " + wordModel.getType());
//			log4j.info("-----");
//		}

		File dictFile = new File("D:\\BooksRepo\\process\\dict\\dict_consolidated_v1.0_EXACT_MATCH.txt");
		TextFileReader reader = new TextFileReader(dictFile);

		String line = null;
		AnalysisWrapper aw = new AnalysisWrapper();

		int count = 0;
		try {
			while ((line = reader.readLine()) != null) {
				TreeSet<String> wordList = getDerivedWords(line);
				if (wordList.size() > 0) {
					String words = GlobalUtils.convertArrayToString(wordList, ", ");
					// System.out.print(count + ", ");
					log4j.trace("{} - From word <{}>, derived : [{}]", count, line, words);
				} else {
					log4j.trace("{} - No words could be derived from {}", count, line);
				}
				count++;
				// log4j.info("Lemmas {}", GlobalUtils.convertArrayToString(LEMMAS, ", "));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * boolean result = analysisWrapper.hasDerivationalSuffix(word);
		 * log4j.info("Result for word '" + word + "': " + result);
		 */
		/*
		 * String word = "yol almak";
		 * 
		 * String quote =
		 * "Bir zamanlar küçük bir köyde yaşayan adam, uzun yıllar boyunca sessiz ve sakin bir hayat sürmüştü. "
		 * + "Ancak, beklenmedik bir şekilde hayatı değişmeye başlamıştı. " +
		 * "Yeni bir maceraya atılma kararı aldı ve o günün sabahında yol almıştı. " +
		 * "Onun için bu, hem bir başlangıç hem de eski bir hayattan kopuş anlamına geliyordu. "
		 * +
		 * "Yola çıkarken yanında sadece birkaç parça eşya vardı, ama umutları büyüktü."
		 * ;
		 * 
		 */
		/*
		 * String word = "beti benzi solmak";
		 * 
		 * String quote =
		 * "Sabah erkenden işe gitmek için yola çıkmıştı, fakat bir süre sonra kendini iyi hissetmemeye başladı. "
		 * + "Öğlen saatlerinde ofisteyken yüzünün rengi iyice solmuştu. " +
		 * "Arkadaşları ona baktığında, beti benzi solmuş gibiydi ve hemen dinlenmesi gerektiğini söylediler."
		 * ;
		 */

		// String word = "gözünden yaşlar boşanmak";
		/*
		 * String word = "açık";
		 * 
		 * String quote = "Uzun zamandır böyle duygusal anlar yaşamamıştı. " +
		 * "Eski bir arkadaşının hikâyesini dinlerken, içinde biriken duygulara engel olamayıp ağlamaya başladı. "
		 * +
		 * "Hikâye açığa çıktıkça, gözlerinden yaşlar boşanıyordu ve kimse onu sakinleştiremedi."
		 * ;
		 * 
		 * 
		 * List<String> results = analysisWrapper.searchInText(word, quote);
		 * 
		 * if (!results.isEmpty()) { for (String result : results) {
		 * log4j.info("Found match: " + result); } } else {
		 * log4j.info("No match found in the quote."); }
		 */
	}

}
