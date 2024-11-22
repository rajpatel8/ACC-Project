package com.searchengine.core.ranking;

import com.searchengine.model.Product;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RankedProduct {
    private final Product product;
    private final double score;
    private double normalizedScore;
    private Map<String, Double> scoreComponents;

    public RankedProduct(Product product, double score) {
        this.product = product;
        this.score = score;
        this.scoreComponents = new HashMap<>();
    }

    public Product getProduct() { return product; }
    public double getScore() { return score; }
    public double getNormalizedScore() { return normalizedScore; }
    public void setNormalizedScore(double normalizedScore) {
        this.normalizedScore = normalizedScore;
    }

    public void addScoreComponent(String component, double value) {
        scoreComponents.put(component, value);
    }

    public Map<String, Double> getScoreComponents() {
        return Collections.unmodifiableMap(scoreComponents);
    }

    @Override
    public String toString() {
        return String.format("%s (Score: %.2f, Normalized: %.2f)",
                product.getName(), score, normalizedScore);
    }
}