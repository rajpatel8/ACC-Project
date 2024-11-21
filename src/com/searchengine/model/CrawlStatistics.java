package com.searchengine.model;

public class CrawlStatistics {
    private int totalPages;
    private int successfulPages;
    private int failedPages;
    private long totalTime;
    private long averagePageTime;
    private int totalLinks;
    private int totalImages;
    private long totalSize;
    private int robotsTxtErrors;
    private int httpErrors;
    private int timeoutErrors;

    // Getters and Setters
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void incrementTotalPages() { this.totalPages++; }

    public int getSuccessfulPages() { return successfulPages; }
    public void setSuccessfulPages(int successfulPages) { this.successfulPages = successfulPages; }
    public void incrementSuccessfulPages() { this.successfulPages++; }

    public int getFailedPages() { return failedPages; }
    public void setFailedPages(int failedPages) { this.failedPages = failedPages; }
    public void incrementFailedPages() { this.failedPages++; }

    public long getTotalTime() { return totalTime; }
    public void setTotalTime(long totalTime) { 
        this.totalTime = totalTime;
        if (successfulPages > 0) {
            this.averagePageTime = totalTime / successfulPages;
        }
    }

    public long getAveragePageTime() { return averagePageTime; }

    public int getTotalLinks() { return totalLinks; }
    public void setTotalLinks(int totalLinks) { this.totalLinks = totalLinks; }
    public void incrementTotalLinks() { this.totalLinks++; }

    public int getTotalImages() { return totalImages; }
    public void setTotalImages(int totalImages) { this.totalImages = totalImages; }
    public void incrementTotalImages() { this.totalImages++; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    public void addToTotalSize(long size) { this.totalSize += size; }

    public int getRobotsTxtErrors() { return robotsTxtErrors; }
    public void setRobotsTxtErrors(int robotsTxtErrors) { this.robotsTxtErrors = robotsTxtErrors; }
    public void incrementRobotsTxtErrors() { this.robotsTxtErrors++; }

    public int getHttpErrors() { return httpErrors; }
    public void setHttpErrors(int httpErrors) { this.httpErrors = httpErrors; }
    public void incrementHttpErrors() { this.httpErrors++; }

    public int getTimeoutErrors() { return timeoutErrors; }
    public void setTimeoutErrors(int timeoutErrors) { this.timeoutErrors = timeoutErrors; }
    public void incrementTimeoutErrors() { this.timeoutErrors++; }
}