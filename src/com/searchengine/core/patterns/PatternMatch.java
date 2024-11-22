package com.searchengine.core.patterns;

public class PatternMatch {
    private final String featureType;
    private final String field;
    private final String matchedText;
    private final int startIndex;
    private final int endIndex;

    public PatternMatch(String featureType, String field, String matchedText,
                        int startIndex, int endIndex) {
        this.featureType = featureType;
        this.field = field;
        this.matchedText = matchedText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    // Getters
    public String getFeatureType() { return featureType; }
    public String getField() { return field; }
    public String getMatchedText() { return matchedText; }
    public int getStartIndex() { return startIndex; }
    public int getEndIndex() { return endIndex; }

    @Override
    public String toString() {
        return String.format("%s found in %s: '%s' at position %d-%d",
                featureType, field, matchedText, startIndex, endIndex);
    }
}