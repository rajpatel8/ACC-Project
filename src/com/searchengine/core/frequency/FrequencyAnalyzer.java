package com.searchengine.core.frequency;

import com.searchengine.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FrequencyAnalyzer {
    private final Map<String, Map<String, Integer>> wordFrequencies;
    private final Map<String, Integer> globalWordFrequencies;
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
        if (products == null) {
            System.out.println("No products to analyze");
            return;
        }

        products.stream()
                .filter(Objects::nonNull)
                .forEach(this::analyzeProduct);

        System.out.println("Analysis completed:");
        System.out.println("- Total unique words: " + globalWordFrequencies.size());
        System.out.println("- Total products analyzed: " + productFrequencies.size());
    }

    private void analyzeProduct(Product product) {
        try {
            if (product == null || product.getProductId() == null) {
                return;
            }

            ProductFrequency freq = new ProductFrequency(product.getProductId());

            // Analyze name
            if (product.getName() != null) {
                analyzeText(product.getName(), product.getProductId(), freq);
            }

            // Analyze features
            if (product.getFeatures() != null) {
                product.getFeatures().stream()
                        .filter(Objects::nonNull)
                        .forEach(feature -> analyzeText(feature, product.getProductId(), freq));
            }

            // Analyze description
            if (product.getDescription() != null) {
                analyzeText(product.getDescription(), product.getProductId(), freq);
            }

            // Analyze specifications
            if (product.getSpecifications() != null) {
                product.getSpecifications().values().stream()
                        .filter(Objects::nonNull)
                        .forEach(spec -> analyzeText(spec, product.getProductId(), freq));
            }

            // Analyze category
            if (product.getCategory() != null) {
                analyzeText(product.getCategory(), product.getProductId(), freq);
            }

            productFrequencies.put(product.getProductId(), freq);
        } catch (Exception e) {
            System.err.println("Error analyzing product: " + e.getMessage());
        }
    }

    private void analyzeText(String text, String productId, ProductFrequency freq) {
        try {
            if (text == null || text.trim().isEmpty() || productId == null || freq == null) {
                return;
            }

            String[] words = text.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", " ")
                    .trim()
                    .split("\\s+");

            for (String word : words) {
                if (word == null || word.length() <= 2 || word.trim().isEmpty()) {
                    continue;
                }

                // Update word frequencies for this product
                Map<String, Integer> productCounts = wordFrequencies.computeIfAbsent(word,
                        k -> new ConcurrentHashMap<>());
                if (productCounts != null) {
                    productCounts.merge(productId, 1, Integer::sum);
                }

                // Update global word frequency
                if (globalWordFrequencies != null) {
                    globalWordFrequencies.merge(word, 1, Integer::sum);
                }

                // Update product frequency
                freq.incrementWordCount(word);
            }
        } catch (Exception e) {
            System.err.println("Error analyzing text: " + e.getMessage());
        }
    }

    public void recordSearch(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return;
            }

            query = query.toLowerCase().trim();
            final String finalQuery = query;
            SearchTerm searchTerm = searchHistory.computeIfAbsent(finalQuery,
                    k -> new SearchTerm(finalQuery));
            if (searchTerm != null) {
                searchTerm.incrementCount();
            }
        } catch (Exception e) {
            System.err.println("Error recording search: " + e.getMessage());
        }
    }

    public List<FrequencyResult> getTopWords(int limit) {
        try {
            if (limit <= 0) {
                return Collections.emptyList();
            }

            return globalWordFrequencies.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .map(e -> new FrequencyResult(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparingInt(FrequencyResult::getFrequency).reversed())
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            System.err.println("Error getting top words: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<SearchTerm> getTopSearches(int limit) {
        try {
            if (limit <= 0) {
                return Collections.emptyList();
            }

            return searchHistory.values().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(SearchTerm::getCount).reversed())
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            System.err.println("Error getting top searches: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int getWordFrequency(String word, String productId) {
        try {
            if (word == null || productId == null) {
                return 0;
            }
            Map<String, Integer> productCounts = wordFrequencies.get(word.toLowerCase());
            return productCounts != null ? productCounts.getOrDefault(productId, 0) : 0;
        } catch (Exception e) {
            System.err.println("Error getting word frequency: " + e.getMessage());
            return 0;
        }
    }

    public int getGlobalWordFrequency(String word) {
        try {
            if (word == null) {
                return 0;
            }
            return globalWordFrequencies.getOrDefault(word.toLowerCase(), 0);
        } catch (Exception e) {
            System.err.println("Error getting global word frequency: " + e.getMessage());
            return 0;
        }
    }

    public Map<String, Integer> getWordFrequenciesForProduct(String productId) {
        try {
            if (productId == null) {
                return Collections.emptyMap();
            }
            ProductFrequency freq = productFrequencies.get(productId);
            return freq != null ? freq.getWordFrequencies() : Collections.emptyMap();
        } catch (Exception e) {
            System.err.println("Error getting word frequencies for product: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}