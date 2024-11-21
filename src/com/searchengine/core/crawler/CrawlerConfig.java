package com.searchengine.core.crawler;

import java.util.HashSet;
import java.util.Set;

import org.openqa.selenium.chrome.ChromeOptions;

public class CrawlerConfig {
    private int maxDepth;
    private int maxPages;
    private int threadCount;
    private String driverPath;
    private int pageLoadTimeout;
    private boolean headless;
    private ChromeOptions chromeOptions;
    private int retryAttempts;
    private long retryDelay;
    private String userAgent;
    private boolean javascriptEnabled;
    private boolean imagesEnabled;
    private String proxyHost;
    private int proxyPort;
    private Set <String> allowedDomains;
    private Set<String> excludedUrls;

    public CrawlerConfig() {
        this.maxDepth = 3;
        this.maxPages = 1000;
        this.threadCount = 4;
        this.driverPath = "/opt/homebrew/bin/chromedriver";
        this.pageLoadTimeout = 30;
        this.headless = false;
        this.retryAttempts = 3;
        this.retryDelay = 1000;
        this.userAgent = "SearchEngineBot/1.0";
        this.javascriptEnabled = true;
        this.imagesEnabled = true;
        this.allowedDomains = new HashSet<>();
        this.excludedUrls = new HashSet<>();
    }

    // Getters and Setters
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public String getDriverPath() { return driverPath; }
    public void setDriverPath(String driverPath) { this.driverPath = driverPath; }

    public int getPageLoadTimeout() { return pageLoadTimeout; }
    public void setPageLoadTimeout(int pageLoadTimeout) { 
        this.pageLoadTimeout = pageLoadTimeout; 
    }

    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { 
        this.headless = headless;
        updateChromeOptions();
    }

    public ChromeOptions getChromeOptions() { return chromeOptions; }
    public void setChromeOptions(ChromeOptions options) { this.chromeOptions = options; }

    public int getRetryAttempts() { return retryAttempts; }
    public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

    public long getRetryDelay() { return retryDelay; }
    public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { 
        this.userAgent = userAgent;
        updateChromeOptions();
    }

    public boolean isJavascriptEnabled() { return javascriptEnabled; }
    public void setJavascriptEnabled(boolean javascriptEnabled) {
        this.javascriptEnabled = javascriptEnabled;
        updateChromeOptions();
    }

    public boolean isImagesEnabled() { return imagesEnabled; }
    public void setImagesEnabled(boolean imagesEnabled) {
        this.imagesEnabled = imagesEnabled;
        updateChromeOptions();
    }

    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { 
        this.proxyHost = proxyHost;
        updateChromeOptions();
    }

    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int proxyPort) { 
        this.proxyPort = proxyPort;
        updateChromeOptions();
    }

    public Set<String> getAllowedDomains() { return allowedDomains; }
    public void setAllowedDomains(Set<String> allowedDomains) { 
        this.allowedDomains = allowedDomains; 
    }
    public void addAllowedDomain(String domain) { 
        this.allowedDomains.add(domain); 
    }

    public Set<String> getExcludedUrls() { return excludedUrls; }
    public void setExcludedUrls(Set<String> excludedUrls) { 
        this.excludedUrls = excludedUrls; 
    }
    public void addExcludedUrl(String url) { 
        this.excludedUrls.add(url); 
    }

    private void updateChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        
        // Headless mode
        if (headless) {
            options.addArguments("--headless");
        }
        
        // Basic options
        options.addArguments(
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-extensions",
            "--window-size=1920,1080"
        );
        
        // JavaScript
        if (!javascriptEnabled) {
            options.addArguments("--disable-javascript");
        }
        
        // Images
        if (!imagesEnabled) {
            options.addArguments("--disable-images");
        }
        
        // User Agent
        options.addArguments("--user-agent=" + userAgent);
        
        // Proxy
        if (proxyHost != null && !proxyHost.isEmpty()) {
            options.addArguments("--proxy-server=" + proxyHost + ":" + proxyPort);
        }
        
        this.chromeOptions = options;
    }
}
