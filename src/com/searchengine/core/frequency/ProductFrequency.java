package com.searchengine.core.frequency;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProductFrequency {
    private final String productId;
    private final Map<String, Integer> wordFrequencies;
    private int totalWords;

    public ProductFrequency(String productId) {
        this.productId = productId;
        this.wordFrequencies = new ConcurrentHashMap<>();
        this.totalWords = 0;
    }

    public void incrementWordCount(String word) {
        wordFrequencies.merge(word, 1, Integer::sum);
        totalWords++;
    }

    public String getProductId() { return productId; }
    public Map<String, Integer> getWordFrequencies() { return wordFrequencies; }
    public int getTotalWords() { return totalWords; }

    public double getWordFrequencyRatio(String word) {
        return totalWords == 0 ? 0 :
                (double) wordFrequencies.getOrDefault(word, 0) / totalWords;
    }
}
