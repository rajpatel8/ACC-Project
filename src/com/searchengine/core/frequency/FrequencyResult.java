package com.searchengine.core.frequency;

public class FrequencyResult {
    private final String word;
    private final int frequency;

    public FrequencyResult(String word, int frequency) {
        this.word = word;
        this.frequency = frequency;
    }

    public String getWord() { return word; }
    public int getFrequency() { return frequency; }

    @Override
    public String toString() {
        return word + " (" + frequency + ")";
    }
}