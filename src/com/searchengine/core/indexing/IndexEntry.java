package com.searchengine.core.indexing;

import java.util.*;

class IndexEntry {
    private final Map<String, PostingList> postings;
    private int documentFrequency;

    public IndexEntry() {
        this.postings = new HashMap<>();
        this.documentFrequency = 0;
    }

    public void addOccurrence(String productId, double weight) {
        PostingList posting = postings.computeIfAbsent(productId,
                k -> new PostingList());
        posting.addOccurrence(weight);

        if (posting.getTermFrequency() == 1) {
            documentFrequency++;
        }
    }

    public Map<String, PostingList> getPostings() { return postings; }
    public int getDocumentFrequency() { return documentFrequency; }
}