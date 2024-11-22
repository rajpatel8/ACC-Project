package com.searchengine.core.indexing;

class PostingList {
    private int termFrequency;
    private double weight;

    public PostingList() {
        this.termFrequency = 0;
        this.weight = 0.0;
    }

    public void addOccurrence(double weight) {
        termFrequency++;
        this.weight += weight;
    }

    public int getTermFrequency() { return termFrequency; }
    public double getWeight() { return weight; }
}