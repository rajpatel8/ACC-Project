package com.searchengine.core.frequency;

import com.searchengine.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

public class FrequencyAnalyzer {
    private final Map<String, Map<String, Integer>> wordFrequencies; // word -> (productId -> count)
    private final Map<String, Integer> globalWordFrequencies; // word -> total count
    private final Map<String, SearchTerm> searchHistory;
    private final Map<String, ProductFrequency> productFrequencies;

    public FrequencyAnalyzer() {
        this.wordFrequencies = new ConcurrentHashMap<>();
        this.globalWordFrequencies = new ConcurrentHashMap<>();
        this.searchHistory = new ConcurrentHashMap<>();
        this.productFrequencies = new ConcurrentHashMap<>();
    }

    public void analyzeProducts(List<Product> products) {
        System.out.println("Analyzing product frequencies...");

        for (Product product : products) {
            analyzeProduct(product);
        }

        // Print some statistics
        System.out.println("Analysis completed:");
        System.out.println("- Total unique words: " + globalWordFrequencies.size());
        System.out.println("- Total products analyzed: " + productFrequencies.size());

        // Print top 10 most frequent words
        System.out.println("\nTop 10 most frequent words:");
        globalWordFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.println("- " + e.getKey() + ": " + e.getValue()));
    }

    private void analyzeProduct(Product product) {
        ProductFrequency freq = new ProductFrequency(product.getProductId());

        // Analyze product name
        analyzeText(product.getName(), product.getProductId(), freq);

        // Analyze features
        for (String feature : product.getFeatures()) {
            analyzeText(feature, product.getProductId(), freq);
        }

        // Analyze description
        analyzeText(product.getDescription(), product.getProductId(), freq);

        // Analyze specifications
        for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
            analyzeText(spec.getValue(), product.getProductId(), freq);
        }

        productFrequencies.put(product.getProductId(), freq);
    }

    private void analyzeText(String text, String productId, ProductFrequency freq) {
        if (text == null) return;

        // Split text into words and clean
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() <= 2) continue; // Skip very short words

            // Update word frequencies for this product
            wordFrequencies
                    .computeIfAbsent(word, k -> new ConcurrentHashMap<>())
                    .merge(productId, 1, Integer::sum);

            // Update global word frequency
            globalWordFrequencies.merge(word, 1, Integer::sum);

            // Update product frequency
            freq.incrementWordCount(word);
        }
    }

    public void recordSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;

        query = query.toLowerCase().trim();
        String finalQuery = query;
        SearchTerm searchTerm = searchHistory.computeIfAbsent(query,
                k -> new SearchTerm(finalQuery));
        searchTerm.incrementCount();
    }

    public int getWordFrequency(String word, String productId) {
        if (word == null || productId == null) return 0;
        return wordFrequencies
                .getOrDefault(word.toLowerCase(), Collections.emptyMap())
                .getOrDefault(productId, 0);
    }

    public int getGlobalWordFrequency(String word) {
        if (word == null) return 0;
        return globalWordFrequencies.getOrDefault(word.toLowerCase(), 0);
    }

    public List<FrequencyResult> getTopWords(int limit) {
        return globalWordFrequencies.entrySet().stream()
                .map(e -> new FrequencyResult(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(FrequencyResult::getFrequency).reversed())
                .limit(limit)
                .toList();
    }

    public List<SearchTerm> getTopSearches(int limit) {
        return searchHistory.values().stream()
                .sorted(Comparator.comparingInt(SearchTerm::getCount).reversed())
                .limit(limit)
                .toList();
    }

    public Map<String, Integer> getWordFrequenciesForProduct(String productId) {
        return productFrequencies
                .getOrDefault(productId, new ProductFrequency(productId))
                .getWordFrequencies();
    }
}
