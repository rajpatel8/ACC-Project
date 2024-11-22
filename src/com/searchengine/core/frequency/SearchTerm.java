package com.searchengine.core.frequency;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchTerm {
    private final String term;
    private final AtomicInteger count;
    private final LocalDateTime firstSearched;
    private LocalDateTime lastSearched;

    public SearchTerm(String term) {
        this.term = term;
        this.count = new AtomicInteger(0);
        this.firstSearched = LocalDateTime.now();
        this.lastSearched = firstSearched;
    }

    public void incrementCount() {
        count.incrementAndGet();
        lastSearched = LocalDateTime.now();
    }

    public String getTerm() { return term; }
    public int getCount() { return count.get(); }
    public LocalDateTime getFirstSearched() { return firstSearched; }
    public LocalDateTime getLastSearched() { return lastSearched; }

    @Override
    public String toString() {
        return String.format("%s (searched %d times, last: %s)",
                term, count.get(), lastSearched);
    }
}