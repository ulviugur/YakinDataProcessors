package com.langpack.process;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.model.AnalysisWrapper;
import com.langpack.model.WordModel;

public class MockPhraseInQuoteChecker {
	
	public static final Logger log4j = LogManager.getLogger("MockPhraseInQuoteChecker");
	
	
	public static void main(String[] args) {
		AnalysisWrapper analysisWrapper = new AnalysisWrapper();
		
		String word = "kitapçıya"; 
		
		List<WordModel> wordModels = analysisWrapper.getWordAnalysis(word);
		
		for (WordModel wordModel : wordModels) {
	        log4j.info("Original Word: " + wordModel.getOriginalWord());
	        log4j.info("Mapped Word: " + wordModel.getMappedWord());
	        log4j.info("Stem: " + wordModel.getStem());
	        log4j.info("Root Word: " + wordModel.getRootWord());
	        log4j.info("Suffix Type: " + wordModel.getSuffixType());
	        log4j.info("Match Type: " + wordModel.getType());
	        log4j.info("-----"); 
	    }
		
		/*
		boolean result = analysisWrapper.hasDerivationalSuffix(word);
		log4j.info("Result for word '" + word + "': " + result);
*/
	/*	
		String word = "yol almak";
        
        String quote = "Bir zamanlar küçük bir köyde yaşayan adam, uzun yıllar boyunca sessiz ve sakin bir hayat sürmüştü. "
        		+ "Ancak, beklenmedik bir şekilde hayatı değişmeye başlamıştı. "
        		+ "Yeni bir maceraya atılma kararı aldı ve o günün sabahında yol almıştı. "
        		+ "Onun için bu, hem bir başlangıç hem de eski bir hayattan kopuş anlamına geliyordu. "
        		+ "Yola çıkarken yanında sadece birkaç parça eşya vardı, ama umutları büyüktü.";

    */ 
    /*    String word = "beti benzi solmak";
        
        String quote = "Sabah erkenden işe gitmek için yola çıkmıştı, fakat bir süre sonra kendini iyi hissetmemeye başladı. "
        		+ "Öğlen saatlerinde ofisteyken yüzünün rengi iyice solmuştu. "
        		+ "Arkadaşları ona baktığında, beti benzi solmuş gibiydi ve hemen dinlenmesi gerektiğini söylediler.";
    */    
        
        //String word = "gözünden yaşlar boşanmak";
	/*	String word = "açık";
        
        String quote = "Uzun zamandır böyle duygusal anlar yaşamamıştı. "
        		+ "Eski bir arkadaşının hikâyesini dinlerken, içinde biriken duygulara engel olamayıp ağlamaya başladı. "
        		+ "Hikâye açığa çıktıkça, gözlerinden yaşlar boşanıyordu ve kimse onu sakinleştiremedi.";
        
        
        List<String> results = analysisWrapper.searchInText(word, quote);
        
        if (!results.isEmpty()) {
        	for (String result : results) {
        		log4j.info("Found match: " + result);
        	}
        } else {
        	log4j.info("No match found in the quote.");
        }*/
	}

}
