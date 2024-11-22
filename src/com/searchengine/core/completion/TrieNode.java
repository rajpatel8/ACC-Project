package com.searchengine.core.completion;

import java.util.*;

class TrieNode {
    private Map<Character, TrieNode> children;
    private boolean isEndOfWord;
    private int frequency;
    private Set<String> productIds;  // Store product IDs where this word appears

    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.frequency = 0;
        this.productIds = new HashSet<>();
    }

    public Map<Character, TrieNode> getChildren() { return children; }
    public boolean isEndOfWord() { return isEndOfWord; }
    public void setEndOfWord(boolean endOfWord) { isEndOfWord = endOfWord; }
    public int getFrequency() { return frequency; }
    public void incrementFrequency() { this.frequency++; }
    public Set<String> getProductIds() { return productIds; }
    public void addProductId(String productId) { this.productIds.add(productId); }
}