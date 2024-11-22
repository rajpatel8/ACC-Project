package com.searchengine.core.indexing;

import java.time.Instant;
import java.util.List;

public class SearchResult {
    private final List<SearchResultItem> items;
    private final Instant timestamp;
    private final long searchTime;

    public SearchResult(List<SearchResultItem> items, Instant timestamp) {
        this.items = items;
        this.timestamp = timestamp;
        this.searchTime = System.currentTimeMillis() - timestamp.toEpochMilli();
    }

    public List<SearchResultItem> getItems() {
        return items;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getSearchTime() {
        return searchTime;
    }
}
