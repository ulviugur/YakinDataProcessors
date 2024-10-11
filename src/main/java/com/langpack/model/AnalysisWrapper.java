package com.langpack.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.InformalAnalysisConverter;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSpellChecker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnalysisWrapper {

	public static final Logger logger = LogManager.getLogger("AnalysisWrapper");
	
	TurkishMorphology morphology = null;
	InformalAnalysisConverter informalConverter = null;
	TurkishSpellChecker spellChecker;

	public AnalysisWrapper() {

		morphology = TurkishMorphology.builder().setLexicon(RootLexicon.getDefault()).useInformalAnalysis().build();
		this.informalConverter = new InformalAnalysisConverter(morphology.getWordGenerator());
		try {
			this.spellChecker = new TurkishSpellChecker(morphology);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<WordModel> getWordAnalysis(String word) {

		List<WordModel> results = new ArrayList<>();
		Set<String> suggestedWords = new HashSet<>(); // To store unique suggestions

		// Step 1: Check if the word exists in the dictionary
		WordAnalysis analysis = morphology.analyze(word);
		if (analysis.analysisCount() > 0) {
			for (SingleAnalysis singleAnalysis : analysis.getAnalysisResults()) {
				String rootWord = extractRootWord(singleAnalysis);
				results.add(
						new WordModel(WordType.EXACT_MATCH, word, word, singleAnalysis.getStems().get(0), rootWord));
				suggestedWords.add(rootWord);
			}
			return results; // Return all correct word analyses
		}

		// Step 2: Perform informal analysis to find a formal equivalent
		List<SingleAnalysis> informalAnalysis = morphology.analyzeAndDisambiguate(word).bestAnalysis();
		if (!informalAnalysis.isEmpty()) {
			for (SingleAnalysis bestInformalAnalysis : informalAnalysis) {
				WordGenerator.Result formalResult = informalConverter.convert(bestInformalAnalysis.surfaceForm(),
						bestInformalAnalysis);
				String formalForm = formalResult.surface;

				// Check if the formalized word exists in the dictionary
				WordAnalysis formalAnalysis = morphology.analyze(formalForm);
				if (formalAnalysis.analysisCount() > 0) {
					for (SingleAnalysis bestFormalAnalysis : formalAnalysis.getAnalysisResults()) {
						String rootWord = extractRootWord(bestFormalAnalysis);
						results.add(new WordModel(WordType.POSSIBLE_MATCH, word, formalForm,
								bestFormalAnalysis.getStems().get(0), rootWord));
						suggestedWords.add(rootWord);
					}
				}
			}
			if (!results.isEmpty()) {
				return results; // Return all possible matches from informal analysis
			}
		}

		// Step 3: Perform spell checking if informal analysis didn't find a match
		List<String> suggestions = spellChecker.suggestForWord(word);
		if (!suggestions.isEmpty()) {
			for (String suggestion : suggestions) {
				WordAnalysis correctedAnalysis = morphology.analyze(suggestion);
				if (correctedAnalysis.analysisCount() > 0) {
					for (SingleAnalysis bestCorrectedAnalysis : correctedAnalysis.getAnalysisResults()) {
						String rootWord = extractRootWord(bestCorrectedAnalysis);
						results.add(new WordModel(WordType.POSSIBLE_MATCH, word, suggestion,
								bestCorrectedAnalysis.getStems().get(0), rootWord));
						suggestedWords.add(rootWord);
					}
				}
			}
			if (!results.isEmpty()) {
				return results; // Return all possible matches from spell checker
			}
		}

		// Step 4: If neither the original nor the normalized versions are found,
		// classify as UNRECOGNIZED_WORD
		results.add(new WordModel(WordType.UNRECOGNIZED_WORD, word, null, null, null));
		return results;
	}

	private String extractRootWord(SingleAnalysis analysis) {
		// Use regex to extract the word inside the brackets from the analysis result
		String analysisString = analysis.formatLong();
		Pattern pattern = Pattern.compile("\\[(.*?):");
		Matcher matcher = pattern.matcher(analysisString);

		if (matcher.find()) {
			return matcher.group(1); // Returns the root word, e.g., 'okumak'
		} else {
			//logger.warn("Could not extract root word from analysis: {}", analysisString);
			return null; // Return null if no match is found
		}
	}

	
// Functions to search word or phrase in text	
	
	public List<String> searchInText(String word, String quote) {
		// Check if the input is a single word or a phrase
		if (isSingleWord(word)) {
			return searchWordInText(word, quote);
		} else {
			return searchPhraseInText(word, quote);
		}
	}
	
	private boolean isSingleWord(String word) {
		return !word.trim().contains(" ");
	}
	
	private List<String> searchWordInText(String targetWord, String quote) {
		List<String> matchedWords = new ArrayList<>();
		
		// Extract the target word's root form
		List<WordAnalysis> targetAnalysis = morphology.analyzeSentence(targetWord);
		String targetRoot = extractRootForm(targetAnalysis);
		
		String[] words = quote.split(" ");
		
		for (String word : words) {
			List<WordAnalysis> wordAnalysis = morphology.analyzeSentence(word);
			String wordRoot = extractRootForm(wordAnalysis);
			
			if (wordRoot.equals(targetRoot)) {
				matchedWords.add(word);
			}
		}
		
		return matchedWords;
	}
	
	private List<String> searchPhraseInText(String targetPhrase, String quote) {
	    List<String> matchedPhrases = new ArrayList<>();
	    
	    // Analyze the target phrase and extract its root form
	    List<WordAnalysis> targetAnalysis = morphology.analyzeSentence(targetPhrase);
	    String targetRoot = extractRootForm(targetAnalysis);
	    
	    // Split the quote into words
	    String[] words = quote.split(" ");
	    
	    // Iterate over the quote to search for the target phrase
	    for (int i = 0; i < words.length; i++) {
	        StringBuilder phraseInQuote = new StringBuilder();
	        int phraseLength = targetAnalysis.size();  // Number of words in the target phrase
	        
	        // Extract roots of consecutive words in the quote
	        for (int j = i; j < i + phraseLength && j < words.length; j++) {
	            List<WordAnalysis> wordAnalysis = morphology.analyzeSentence(words[j]);
	            String wordRoot = extractRootForm(wordAnalysis);
	            phraseInQuote.append(wordRoot).append(" ");
	        }
	        
	        String phraseInQuoteRoot = phraseInQuote.toString().trim();
	        
	        // Check if the root form matches the target root form
	        if (phraseInQuoteRoot.contains(targetRoot)) {
	            StringBuilder matchedPhrase = new StringBuilder();
	            for (int j = i; j < i + phraseLength && j < words.length; j++) {
	                matchedPhrase.append(words[j]).append(" ");
	            }
	            matchedPhrases.add(matchedPhrase.toString().trim());
	            i += phraseLength - 1;  // Skip the words in the found phrase
	        }
	    }
	    
	    return matchedPhrases;
	}

	private String extractRootForm(List<WordAnalysis> analysisList) {
		StringBuilder rootForm = new StringBuilder();
		for (WordAnalysis analysis : analysisList) {
			for (SingleAnalysis single : analysis) {
				String root = single.getLemmas().get(0);
				rootForm.append(root).append(" ");
				break;
			}
		}
		
		return rootForm.toString().trim();
	}

}
