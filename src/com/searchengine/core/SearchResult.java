package com.searchengine.core;

import java.util.*;
import com.searchengine.core.completion.WordCompletion.Suggestion;
import com.searchengine.core.indexing.SearchResultItem;
import com.searchengine.core.ranking.RankedProduct;

public class SearchResult {
    private List<String> spellingSuggestions;
    private List<Suggestion> completions;
    private List<SearchResultItem> indexResults;
    private List<RankedProduct> rankedResults;
    private long searchTime;

    public SearchResult() {
        this.spellingSuggestions = new ArrayList<>();
        this.completions = new ArrayList<>();
        this.rankedResults = new ArrayList<>();
        this.searchTime = System.currentTimeMillis();
    }

    public void addSpellingSuggestions(List<String> suggestions) {
        this.spellingSuggestions.addAll(suggestions);
    }

    public void setCompletions(List<Suggestion> completions) {
        this.completions = completions;
    }

    public void setIndexResults(List<SearchResultItem> indexResults) {
        this.indexResults = indexResults;
    }

    public void setRankedResults(List<RankedProduct> rankedResults) {
        this.rankedResults = rankedResults;
    }

    // Getters
    public List<String> getSpellingSuggestions() { return spellingSuggestions; }
    public List<Suggestion> getCompletions() { return completions; }
    public List<SearchResultItem> getIndexResults() { return indexResults; }
    public List<RankedProduct> getRankedResults() { return rankedResults; }
    public long getSearchTime() { return System.currentTimeMillis() - searchTime; }
}