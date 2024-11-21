package com.searchengine.model;

import java.util.List;

public class SearchResult {
    private List<Document> documents;
    private List<String> suggestions;
    private long searchTime;
    
    public SearchResult(List<Document> documents, List<String> suggestions) {
        this.documents = documents;
        this.suggestions = suggestions;
        this.searchTime = System.currentTimeMillis();
    }
    
    // Getters and setters
    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { 
        this.documents = documents; 
    }
    
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { 
        this.suggestions = suggestions; 
    }
    
    public long getSearchTime() { return searchTime; }
    public void setSearchTime(long searchTime) { 
        this.searchTime = searchTime; 
    }
}