package com.searchengine.model;

import java.util.*;

public class CrawlSession {
    private String sessionId;
    private long startTime;
    private long endTime;
    private int pagesProcessed;
    private int totalLinks;
    private Set<String> visitedUrls;
    private Queue<String> pendingUrls;
    private Map<String, CrawlError> errors;
    private CrawlStatistics statistics;

    public CrawlSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        this.visitedUrls = new HashSet<>();
        this.pendingUrls = new LinkedList<>();
        this.errors = new HashMap<>();
        this.statistics = new CrawlStatistics();
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getPagesProcessed() { return pagesProcessed; }
    public void incrementPagesProcessed() { this.pagesProcessed++; }

    public int getTotalLinks() { return totalLinks; }
    public void setTotalLinks(int totalLinks) { this.totalLinks = totalLinks; }

    public Set<String> getVisitedUrls() { return visitedUrls; }
    public void addVisitedUrl(String url) { this.visitedUrls.add(url); }

    public Queue<String> getPendingUrls() { return pendingUrls; }
    public void addPendingUrl(String url) { this.pendingUrls.offer(url); }

    public Map<String, CrawlError> getErrors() { return errors; }
    public void addError(String url, CrawlError error) { this.errors.put(url, error); }

    public CrawlStatistics getStatistics() { return statistics; }
    public void setStatistics(CrawlStatistics statistics) { this.statistics = statistics; }

    public long getDuration() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
}
