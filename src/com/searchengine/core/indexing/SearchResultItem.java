package com.searchengine.core.indexing;

import com.searchengine.model.Product;
import java.util.Set;

public class SearchResultItem {
    private final Product product;
    private final double score;
    private final Set<String> matchedTerms;

    public SearchResultItem(Product product, double score, Set<String> matchedTerms) {
        this.product = product;
        this.score = score;
        this.matchedTerms = matchedTerms;
    }

    public Product getProduct() { return product; }
    public double getScore() { return score; }
    public Set<String> getMatchedTerms() { return matchedTerms; }
}