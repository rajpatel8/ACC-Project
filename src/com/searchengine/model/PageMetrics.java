package com.searchengine.model;

public class PageMetrics {
    private int wordCount;
    private int characterCount;
    private int linkCount;
    private int imageCount;
    private long loadTime;
    private int statusCode;
    private long documentSize;
    private boolean hasIframes;
    private boolean hasJavaScript;
    private boolean hasForms;

    // Getters and Setters
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getCharacterCount() { return characterCount; }
    public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

    public int getLinkCount() { return linkCount; }
    public void setLinkCount(int linkCount) { this.linkCount = linkCount; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public long getLoadTime() { return loadTime; }
    public void setLoadTime(long loadTime) { this.loadTime = loadTime; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public long getDocumentSize() { return documentSize; }
    public void setDocumentSize(long documentSize) { this.documentSize = documentSize; }

    public boolean isHasIframes() { return hasIframes; }
    public void setHasIframes(boolean hasIframes) { this.hasIframes = hasIframes; }

    public boolean isHasJavaScript() { return hasJavaScript; }
    public void setHasJavaScript(boolean hasJavaScript) { this.hasJavaScript = hasJavaScript; }

    public boolean isHasForms() { return hasForms; }
    public void setHasForms(boolean hasForms) { this.hasForms = hasForms; }
}

// File: src/com/searchengine/model/CrawlSession.java
