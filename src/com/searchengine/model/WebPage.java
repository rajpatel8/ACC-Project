package com.searchengine.model;

import java.util.*;

public class WebPage {
    private String url;
    private String title;
    private String content;
    private Map<String, String> metadata;
    private Set<String> links;
    private List<String> images;
    private long crawlTimestamp;
    private Map<String, String> headers;
    private String metaDescription;
    private String metaKeywords;
    private PageMetrics metrics;

    public WebPage() {
        this.metadata = new HashMap<>();
        this.links = new HashSet<>();
        this.images = new ArrayList<>();
        this.headers = new HashMap<>();
        this.crawlTimestamp = System.currentTimeMillis();
        this.metrics = new PageMetrics();
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, String value) { this.metadata.put(key, value); }

    public Set<String> getLinks() { return links; }
    public void setLinks(Set<String> links) { this.links = links; }
    public void addLink(String link) { this.links.add(link); }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public void addImage(String image) { this.images.add(image); }

    public long getCrawlTimestamp() { return crawlTimestamp; }
    public void setCrawlTimestamp(long crawlTimestamp) { this.crawlTimestamp = crawlTimestamp; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void addHeader(String key, String value) { this.headers.put(key, value); }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public String getMetaKeywords() { return metaKeywords; }
    public void setMetaKeywords(String metaKeywords) { this.metaKeywords = metaKeywords; }

    public PageMetrics getMetrics() { return metrics; }
    public void setMetrics(PageMetrics metrics) { this.metrics = metrics; }
}

// File: src/com/searchengine/model/PageMetrics.java

