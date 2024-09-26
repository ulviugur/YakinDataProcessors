package com.langpack.model;

import java.util.List;

public class AnalysisResult {
	private final List<String> morphemes;
    private final List<String> descriptions;

    public AnalysisResult(List<String> morphemes, List<String> descriptions) {
        this.morphemes = morphemes;
        this.descriptions = descriptions;
    }

    public List<String> getMorphemes() {
        return morphemes;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }
}
