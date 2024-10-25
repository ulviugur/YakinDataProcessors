package com.langpack.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.normalization.TurkishSpellChecker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.StringProcessor;
import com.langpack.model.suffix.SuffixType;

public class AnalysisWrapper2 {

	public static final Logger log4j = LogManager.getLogger("AnalysisWrapper");
	
	TurkishMorphology morphology = null;
	InformalAnalysisConverter informalConverter = null;
	TurkishSpellChecker spellChecker;
	
	public static WordGenerator gen = null;

	public AnalysisWrapper2() {

		morphology = TurkishMorphology.builder().setLexicon(RootLexicon.getDefault()).useInformalAnalysis().build();
		gen = morphology.getWordGenerator();
		this.informalConverter = new InformalAnalysisConverter(morphology.getWordGenerator());
		try {
			this.spellChecker = new TurkishSpellChecker(morphology);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TreeSet<String> getDerivedWords(String _word) {
		String word = StringProcessor.removePunctuationForAnalysis(_word);
		//String rr2 = StringProcessor.removeSpecialCharacters(word);
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
			log4j.trace("Analiz {} : {}", count, singleAnalysis.toString());
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
		//log4j.info("Derived list {} : {}", word, GlobalUtils.convertArraytoString(derivedWords));
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

}
