package com.searchengine.core.ranking;

import com.searchengine.model.Product;
import com.searchengine.core.frequency.FrequencyAnalyzer;
import java.util.*;

public class PageRanker {
    private final FrequencyAnalyzer frequencyAnalyzer;
    private final Map<String, Double> productScores;
    private final Map<String, Double> categoryBoosts;

    // Ranking weights
    private static final double TITLE_WEIGHT = 0.4;
    private static final double DESCRIPTION_WEIGHT = 0.3;
    private static final double FEATURES_WEIGHT = 0.2;
    private static final double SPEC_WEIGHT = 0.1;

    public PageRanker(FrequencyAnalyzer frequencyAnalyzer) {
        this.frequencyAnalyzer = frequencyAnalyzer;
        this.productScores = new HashMap<>();
        this.categoryBoosts = new HashMap<>();
        initializeCategoryBoosts();
    }

    private void initializeCategoryBoosts() {
        // Define category importance weights
        categoryBoosts.put("premium", 1.2);
        categoryBoosts.put("wireless", 1.1);
        categoryBoosts.put("bluetooth", 1.1);
        categoryBoosts.put("standard", 1.0);
    }

    public List<RankedProduct> rankProducts(List<Product> products, String searchQuery) {
        List<RankedProduct> rankedProducts = new ArrayList<>();
        String[] queryTerms = preprocessQuery(searchQuery);

        for (Product product : products) {
            double score = calculateProductScore(product, queryTerms);
            rankedProducts.add(new RankedProduct(product, score));
        }

        // Sort by score in descending order
        rankedProducts.sort((p1, p2) -> Double.compare(p2.getScore(), p1.getScore()));

        // Calculate normalized scores
        if (!rankedProducts.isEmpty()) {
            double maxScore = rankedProducts.get(0).getScore();
            for (RankedProduct product : rankedProducts) {
                product.setNormalizedScore(product.getScore() / maxScore);
            }
        }

        return rankedProducts;
    }

    private String[] preprocessQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .trim()
                .split("\\s+");
    }

    private double calculateProductScore(Product product, String[] queryTerms) {
        double score = 0.0;

        // Calculate title score
        score += TITLE_WEIGHT * calculateFieldScore(product.getName(), queryTerms);

        // Calculate description score
        score += DESCRIPTION_WEIGHT * calculateFieldScore(product.getDescription(), queryTerms);

        // Calculate features score
        double featuresScore = 0.0;
        for (String feature : product.getFeatures()) {
            featuresScore += calculateFieldScore(feature, queryTerms);
        }
        score += FEATURES_WEIGHT * (featuresScore / Math.max(1, product.getFeatures().size()));

        // Calculate specifications score
        double specsScore = 0.0;
        for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
            specsScore += calculateFieldScore(spec.getValue(), queryTerms);
        }
        score += SPEC_WEIGHT * (specsScore / Math.max(1, product.getSpecifications().size()));

        // Apply category boost
        score *= getCategoryBoost(product);

        // Apply frequency boost
        score *= getFrequencyBoost(product, queryTerms);

        return score;
    }

    private double calculateFieldScore(String field, String[] queryTerms) {
        if (field == null) return 0.0;

        field = field.toLowerCase();
        double score = 0.0;

        for (String term : queryTerms) {
            // Exact match bonus
            if (field.contains(" " + term + " ") ||
                    field.startsWith(term + " ") ||
                    field.endsWith(" " + term)) {
                score += 1.0;
            }

            // Partial match
            else if (field.contains(term)) {
                score += 0.5;
            }

            // Word starts with term
            else {
                String[] words = field.split("\\s+");
                for (String word : words) {
                    if (word.startsWith(term)) {
                        score += 0.3;
                    }
                }
            }
        }

        return score;
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
        // Logarithmic scaling to prevent frequency from dominating the score
        return 1.0 + Math.log1p(totalFrequency);
    }
}
