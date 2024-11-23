package com.searchengine.core.ranking;

import com.searchengine.model.Product;
import com.searchengine.core.frequency.FrequencyAnalyzer;
import java.util.*;

public class PageRanker {
    private final FrequencyAnalyzer frequencyAnalyzer;
    private final Map<String, Double> productScores;
    private final Map<String, Double> categoryBoosts;

    // Ranking weights for different sections
    private static final double TITLE_WEIGHT = 0.4;
    private static final double DESCRIPTION_WEIGHT = 0.3;
    private static final double FEATURES_WEIGHT = 0.2;
    private static final double SPEC_WEIGHT = 0.1;

    // Term frequency weights
    private static final double TF_NORMALIZATION_FACTOR = 0.4;
    private static final double IDF_WEIGHT = 0.6;
    private static final double FREQUENCY_BOOST_FACTOR = 1.5;

    public PageRanker(FrequencyAnalyzer frequencyAnalyzer) {
        this.frequencyAnalyzer = frequencyAnalyzer;
        this.productScores = new HashMap<>();
        this.categoryBoosts = new HashMap<>();
        initializeCategoryBoosts();
    }

    public List<RankedProduct> rankProducts(List<Product> products, String searchQuery) {
        List<RankedProduct> rankedProducts = new ArrayList<>();
        String[] queryTerms = preprocessQuery(searchQuery);
        Map<String, Double> idfScores = calculateIDFScores(products, queryTerms);

        for (Product product : products) {
            double score = calculateProductScore(product, queryTerms, idfScores);
            RankedProduct rankedProduct = new RankedProduct(product, score);

            // Add detailed scoring components for transparency
            Map<String, Double> scoreComponents = calculateScoreComponents(product, queryTerms, idfScores);
            for (Map.Entry<String, Double> component : scoreComponents.entrySet()) {
                rankedProduct.addScoreComponent(component.getKey(), component.getValue());
            }

            rankedProducts.add(rankedProduct);
        }

        // Sort by score in descending order
        rankedProducts.sort((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()));

        // Calculate normalized scores
        normalizeScores(rankedProducts);

        return rankedProducts;
    }

    private Map<String, Double> calculateIDFScores(List<Product> products, String[] queryTerms) {
        Map<String, Double> idfScores = new HashMap<>();
        int totalProducts = products.size();

        for (String term : queryTerms) {
            int documentFrequency = 0;
            for (Product product : products) {
                if (containsTerm(product, term)) {
                    documentFrequency++;
                }
            }
            double idf = Math.log((double) totalProducts / (1 + documentFrequency));
            idfScores.put(term, idf);
        }

        return idfScores;
    }

    private double calculateProductScore(Product product, String[] queryTerms, Map<String, Double> idfScores) {
        double score = 0.0;

        // Calculate TF-IDF scores for different sections
        Map<String, Integer> termFrequencies = calculateTermFrequencies(product, queryTerms);

        for (String term : queryTerms) {
            double tf = termFrequencies.getOrDefault(term, 0);
            double normalizedTf = 1 + Math.log(tf + 1); // Add smoothing
            double idf = idfScores.getOrDefault(term, 0.0);

            // Calculate section-specific scores
            score += TITLE_WEIGHT * calculateSectionScore(product.getName(), term, normalizedTf, idf);
            score += DESCRIPTION_WEIGHT * calculateSectionScore(product.getDescription(), term, normalizedTf, idf);
            score += FEATURES_WEIGHT * calculateFeaturesScore(product.getFeatures(), term, normalizedTf, idf);
            score += SPEC_WEIGHT * calculateSpecificationsScore(product.getSpecifications(), term, normalizedTf, idf);
        }

        // Apply category boost
        score *= getCategoryBoost(product);

        // Apply frequency boost based on historical data
        score *= getFrequencyBoost(product, queryTerms);

        return score;
    }

    private Map<String, Double> calculateScoreComponents(Product product, String[] queryTerms, Map<String, Double> idfScores) {
        Map<String, Double> components = new HashMap<>();

        // Calculate individual components
        double titleScore = 0.0;
        double descScore = 0.0;
        double featuresScore = 0.0;
        double specsScore = 0.0;

        for (String term : queryTerms) {
            Map<String, Integer> termFreq = calculateTermFrequencies(product, new String[]{term});
            double tf = termFreq.getOrDefault(term, 0);
            double normalizedTf = 1 + Math.log(tf + 1);
            double idf = idfScores.getOrDefault(term, 0.0);

            titleScore += calculateSectionScore(product.getName(), term, normalizedTf, idf);
            descScore += calculateSectionScore(product.getDescription(), term, normalizedTf, idf);
            featuresScore += calculateFeaturesScore(product.getFeatures(), term, normalizedTf, idf);
            specsScore += calculateSpecificationsScore(product.getSpecifications(), term, normalizedTf, idf);
        }

        components.put("titleScore", titleScore * TITLE_WEIGHT);
        components.put("descriptionScore", descScore * DESCRIPTION_WEIGHT);
        components.put("featuresScore", featuresScore * FEATURES_WEIGHT);
        components.put("specificationsScore", specsScore * SPEC_WEIGHT);
        components.put("categoryBoost", getCategoryBoost(product));
        components.put("frequencyBoost", getFrequencyBoost(product, queryTerms));

        return components;
    }

    private double calculateSectionScore(String text, String term, double normalizedTf, double idf) {
        if (text == null) return 0.0;
        text = text.toLowerCase();

        double score = 0.0;

        // Exact match bonus
        if (text.contains(" " + term + " ") ||
                text.startsWith(term + " ") ||
                text.endsWith(" " + term)) {
            score += 2.0;
        }

        // Term frequency score
        score += normalizedTf * TF_NORMALIZATION_FACTOR;

        // IDF score
        score += idf * IDF_WEIGHT;

        return score;
    }

    private double calculateFeaturesScore(List<String> features, String term, double normalizedTf, double idf) {
        double score = 0.0;
        for (String feature : features) {
            score += calculateSectionScore(feature, term, normalizedTf, idf);
        }
        return score / Math.max(1, features.size());
    }

    private double calculateSpecificationsScore(Map<String, String> specifications, String term, double normalizedTf, double idf) {
        double score = 0.0;
        for (Map.Entry<String, String> spec : specifications.entrySet()) {
            score += calculateSectionScore(spec.getValue(), term, normalizedTf, idf);
        }
        return score / Math.max(1, specifications.size());
    }

    private Map<String, Integer> calculateTermFrequencies(Product product, String[] terms) {
        Map<String, Integer> frequencies = new HashMap<>();

        for (String term : terms) {
            int freq = 0;

            // Count in name
            freq += countOccurrences(product.getName(), term);

            // Count in description
            freq += countOccurrences(product.getDescription(), term);

            // Count in features
            for (String feature : product.getFeatures()) {
                freq += countOccurrences(feature, term);
            }

            // Count in specifications
            for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                freq += countOccurrences(spec.getValue(), term);
            }

            frequencies.put(term, freq);
        }

        return frequencies;
    }

    private int countOccurrences(String text, String term) {
        if (text == null) return 0;
        text = text.toLowerCase();
        term = term.toLowerCase();

        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }

    private boolean containsTerm(Product product, String term) {
        term = term.toLowerCase();

        if (product.getName() != null && product.getName().toLowerCase().contains(term)) return true;
        if (product.getDescription() != null && product.getDescription().toLowerCase().contains(term)) return true;

        for (String feature : product.getFeatures()) {
            if (feature.toLowerCase().contains(term)) return true;
        }

        for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
            if (spec.getValue().toLowerCase().contains(term)) return true;
        }

        return false;
    }

    private void normalizeScores(List<RankedProduct> products) {
        if (products.isEmpty()) return;

        double maxScore = products.get(0).getScore();
        for (RankedProduct product : products) {
            product.setNormalizedScore(product.getScore() / maxScore);
        }
    }

    // Existing helper methods remain the same
    private String[] preprocessQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .trim()
                .split("\\s+");
    }

    private void initializeCategoryBoosts() {
        categoryBoosts.put("premium", 1.2);
        categoryBoosts.put("wireless", 1.1);
        categoryBoosts.put("bluetooth", 1.1);
        categoryBoosts.put("standard", 1.0);
    }

    private double getCategoryBoost(Product product) {
        String category = product.getCategory().toLowerCase();
        return categoryBoosts.entrySet().stream()
                .filter(entry -> category.contains(entry.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(1.0);
    }

    private double getFrequencyBoost(Product product, String[] queryTerms) {
        double totalFrequency = 0.0;
        for (String term : queryTerms) {
            totalFrequency += frequencyAnalyzer.getWordFrequency(term, product.getProductId());
        }
        return 1.0 + Math.log1p(totalFrequency) * FREQUENCY_BOOST_FACTOR;
    }
}