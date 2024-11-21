package com.searchengine.model;

public class CrawlError {
    private String url;
    private String errorMessage;
    private String errorType;
    private long timestamp;
    private String stackTrace;
    private int attemptCount;

    public CrawlError(String url, String errorMessage, String errorType) {
        this.url = url;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.timestamp = System.currentTimeMillis();
        this.attemptCount = 1;
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public int getAttemptCount() { return attemptCount; }
    public void incrementAttemptCount() { this.attemptCount++; }
}
