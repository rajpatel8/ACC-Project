package com.searchengine.core.cache;

public class CacheConfig {
    private String cacheDirectory;
    private long cacheExpirationHours;
    private boolean cacheEnabled;
    private long maxCacheSize; // in bytes

    public CacheConfig() {
        this.cacheDirectory = "crawler_cache";
        this.cacheExpirationHours = 24; // 24 hours default
        this.cacheEnabled = true;
        this.maxCacheSize = 1024 * 1024 * 1024; // 1GB default
    }

    // Getters and setters
    public String getCacheDirectory() { return cacheDirectory; }
    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public long getCacheExpirationHours() { return cacheExpirationHours; }
    public void setCacheExpirationHours(long cacheExpirationHours) {
        this.cacheExpirationHours = cacheExpirationHours;
    }

    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public long getMaxCacheSize() { return maxCacheSize; }
    public void setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
}