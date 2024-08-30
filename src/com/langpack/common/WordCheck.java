package com.langpack.common;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.normalization.TurkishSpellChecker;

public class WordCheck {
	
	public static final Logger log4j = LogManager.getLogger("WordCheck");
	private final TurkishMorphology morphology;
	private final InformalAnalysisConverter informalConverter;
	private final TurkishSpellChecker spellChecker;
    private final TurkishSentenceNormalizer normalizer;
    
	public WordCheck() throws IOException {
		this.morphology = TurkishMorphology.builder()
				.setLexicon(RootLexicon.getDefault())
				.useInformalAnalysis()
				.build();
		this.informalConverter = new InformalAnalysisConverter(morphology.getWordGenerator());
		this.spellChecker = new TurkishSpellChecker(morphology);
		
		// Load normalization resources
        Path lookupRoot = Paths.get("C:/burcu/zemberek-files/normalization-20240826T124105Z-001/normalization");
        Path lmFile = Paths.get("C:/burcu/zemberek-files/lm-20240826T124056Z-001/lm/lm.2gram.slm");
        this.normalizer = new TurkishSentenceNormalizer(morphology, lookupRoot, lmFile);
	}
	
	 public WordModel checkWord(String word) throws IOException {
		// Step 1: Check if the word exists in the dictionary
	        WordAnalysis analysis = morphology.analyze(word);
	        if (analysis.analysisCount() > 0) {
	            SingleAnalysis bestAnalysis = analysis.getAnalysisResults().get(0);
	            return new WordModel(WordType.EXACT_MATCH, word, word, bestAnalysis.getStems().get(0));
	        }

	     // Step 2: Perform informal analysis and get possible correction
	        List<SingleAnalysis> informalAnalysis = morphology.analyzeAndDisambiguate(word).bestAnalysis();
	        if (!informalAnalysis.isEmpty()) {
	            SingleAnalysis bestInformalAnalysis = informalAnalysis.get(0);
	            WordGenerator.Result formalResult = informalConverter.convert(bestInformalAnalysis.surfaceForm(), bestInformalAnalysis);
	            String formalForm = formalResult.surface;

	            // Check if the formalized word exists in the dictionary
	            WordAnalysis formalAnalysis = morphology.analyze(formalForm);
	            if (formalAnalysis.analysisCount() > 0) {
	                SingleAnalysis bestFormalAnalysis = formalAnalysis.getAnalysisResults().get(0);
	                return new WordModel(WordType.POSSIBLE_MATCH, word, formalForm, bestFormalAnalysis.getStems().get(0));
	            }
	        }

	        // Step 3: If neither the original nor the normalized versions are found, classify as UNRECOGNIZED_WORD
	        return new WordModel(WordType.UNRECOGNIZED_WORD, word, null, null);
	    }


	    public static void main(String[] args) throws IOException {
	    	final Logger log4j = LogManager.getLogger("WordCheck");
	    	TurkishMorphology morphology = TurkishMorphology.builder()
					.setLexicon(RootLexicon.getDefault())
					.useInformalAnalysis()
					.build();
	    	TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);
	    	
	    	/*
	    	RootLexicon i = RootLexicon.getDefault();
	    	Collection<DictionaryItem> j = i.getAllItems();
	    	Iterator<DictionaryItem> k = j.iterator();
	    	int count = 0;
	    	while (k.hasNext()) {
	    		DictionaryItem x = k.next();
	    		EnumSet<RootAttribute> attributes = x.attributes;
	    		String lemma = x.lemma;
	    		PrimaryPos primaryPos = x.primaryPos;
	    		String pronunciation = x.pronunciation;
	    		String root = x.root;
	    		SecondaryPos secondaryPos = x.secondaryPos;
	    		
	    		k.next();
	    		count++;
	    		log4j.info("{}, {}",count, root);
	    	}
	    	System.exit(0);
	    	*/
	    	WordCheck wordCheck = new WordCheck();
	    	
	    	

	        // Example words to analyze
	        String[] words = {"okuycam", "şüretmek", "duruyomuş", ".durgun", "kitap", "yetkinleştiğini", "aaaannnnneeee", "görüyom", "gidicem"};

	        // Analyze each word and print the result
	        for (String word : words) {
	            WordModel result = wordCheck.checkWord(word);
	            System.out.println("Word: " + result.getOriginalWord());
	            System.out.println("Type: " + result.getType());
	            System.out.println("Mapped Word: " + (result.getMappedWord() != null ? result.getMappedWord() : "N/A"));
	            System.out.println("Root Word: " + (result.getRootWord() != null ? result.getRootWord() : "N/A"));
	            System.out.println("Word: " + spellChecker.suggestForWord(word));
	         // Print the first analysis result
	            if (result.getType() == WordType.EXACT_MATCH || result.getType() == WordType.POSSIBLE_MATCH) {
	                WordAnalysis wordAnalysis = wordCheck.morphology.analyze(result.getMappedWord() != null ? result.getMappedWord() : word);
	                if (wordAnalysis.analysisCount() > 0) {
	                    SingleAnalysis firstAnalysis = wordAnalysis.getAnalysisResults().get(0);
	                    System.out.println("First Analysis: " + firstAnalysis);
	                }
	            }
	            
	            System.out.println("-------------");
	        }
	    }
}
