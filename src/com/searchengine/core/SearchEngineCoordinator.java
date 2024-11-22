package com.searchengine.core;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.model.Product;
import java.util.List;

public class SearchEngineCoordinator {
    private final SearchEngine searchEngine;

    public SearchEngineCoordinator() {
        this.searchEngine = new SearchEngine();
    }

    public void initialize() {
        searchEngine.initialize();
    }

    public void addProducts(List<Product> products) {
        searchEngine.addProducts(products);
    }

    public SearchResult search(String query) {
        return searchEngine.search(query);
    }

    public void shutdown() {
        searchEngine.shutdown();
    }
}