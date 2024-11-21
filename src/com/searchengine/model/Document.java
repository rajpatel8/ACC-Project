package com.searchengine.model;

public class Document {
    private String url;
    private String content;
    private long timestamp;
    
    public Document(String url, String content) {
        this.url = url;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
