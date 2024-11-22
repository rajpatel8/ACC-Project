package com.searchengine.core.indexing;

import com.searchengine.model.Product;
import java.util.*;

class ProductDocument {
    private final Product product;
    private final Map<String, Double> termWeights;
    private final Map<String, Integer> termFrequencies;

    public ProductDocument(Product product) {
        this.product = product;
        this.termWeights = new HashMap<>();
        this.termFrequencies = new HashMap<>();
    }

    public void addTerm(String term, double weight) {
        termWeights.merge(term, weight, Double::sum);
        termFrequencies.merge(term, 1, Integer::sum);
    }

    public Product getProduct() { return product; }
    public Map<String, Double> getTermWeights() { return termWeights; }
    public Map<String, Integer> getTermFrequencies() { return termFrequencies; }
}