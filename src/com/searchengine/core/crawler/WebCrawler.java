// File: src/com/searchengine/core/crawler/WebCrawler.java
package com.searchengine.core.crawler;

import com.searchengine.core.cache.PageCache;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

import com.searchengine.model.*;

public class WebCrawler {
    private final CrawlSession session;
    private final ExecutorService executorService;
    private final CrawlerConfig config;
    private final List<CrawlerObserver> observers;
    private volatile boolean isRunning;
    private final Map<Long, WebDriver> drivers;
    private PageCache cache = null;



    public WebCrawler(CrawlerConfig config) {
        this.config = config;
        this.session = new CrawlSession();
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        this.observers = new ArrayList<>();
        this.isRunning = false;
        this.drivers = new ConcurrentHashMap<>();
        setupWebDriver();
    }

    public WebCrawler(CrawlerConfig config, PageCache cache) {
        this.config = config;
        this.session = new CrawlSession();
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        this.observers = new ArrayList<>();
        this.isRunning = false;
        this.drivers = new ConcurrentHashMap<>();
        this.cache = cache;

        setupWebDriver();
    }

    private void setupWebDriver() {
        // Set up ChromeDriver system property
        System.setProperty("webdriver.chrome.driver", config.getDriverPath());
        
        // Set up Chrome options globally
        ChromeOptions options = new ChromeOptions();
        if (config.isHeadless()) {
            options.addArguments("--headless");
        }
        options.addArguments(
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-extensions",
            "--window-size=1920,1080"
        );
        config.setChromeOptions(options);
    }

    private WebDriver getOrCreateDriver() {
        long threadId = Thread.currentThread().getId();
        return drivers.computeIfAbsent(threadId, k -> new ChromeDriver(config.getChromeOptions()));
    }

    public void startCrawling(String seedUrl) {
        if (isRunning) {
            throw new IllegalStateException("Crawler is already running");
        }
        isRunning = true;
        session.setStartTime(System.currentTimeMillis());
        session.addPendingUrl(seedUrl);
        
        startCrawlingProcess();
    }

    private void startCrawlingProcess() {
        while (isRunning && 
               !session.getPendingUrls().isEmpty() && 
               session.getPagesProcessed() < config.getMaxPages()) {
            
            String url = session.getPendingUrls().poll();
            if (url != null && !session.getVisitedUrls().contains(url)) {
              
                processCrawlRequest(url);
            }
            if (session.getPagesProcessed() >= config.getMaxPages()) {
                break;
            }
        }

        if (isRunning) {
            stopCrawling();
        }
    }

    // private void processCrawlRequest(String url) {
    //     executorService.submit(() -> {
    //         try {
                
    //             if (!isRunning) return;
    //             WebDriver driver = getOrCreateDriver();
    //             long startTime = System.currentTimeMillis();
                
    //             WebPage page = crawlPage(driver, url);
                
    //             if (page != null) {
    //                 long endTime = System.currentTimeMillis();
    //                 page.getMetrics().setLoadTime(endTime - startTime);
                    
    //                 session.addVisitedUrl(url);
    //                 session.incrementPagesProcessed();
    //                 session.getStatistics().incrementSuccessfulPages();
                    
    //                 processNewUrls(page.getLinks());
    //                 notifyObservers(page);
    //             }
    //         } catch (Exception e) {
    //             handleCrawlError(url, e);
    //         }
    //     });
    // }

    // private WebPage crawlPage(WebDriver driver, String url) {
    //     try {
    //         WebPage page = new WebPage();
    //         page.setUrl(url);

    //         // Start page load timing
    //         long startTime = System.currentTimeMillis();
            
    //         // Navigate to page
    //         driver.get(url);
            
    //         // Wait for page load
    //         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(config.getPageLoadTimeout()));
    //         wait.until(webDriver -> ((JavascriptExecutor) webDriver)
    //             .executeScript("return document.readyState")
    //             .equals("complete"));

    //         // Extract page information
    //         extractPageInformation(driver, page);
            
    //         // Calculate load time
    //         long endTime = System.currentTimeMillis();
    //         page.getMetrics().setLoadTime(endTime - startTime);

    //         return page;
    //     } catch (Exception e) {
    //         handleCrawlError(url, e);
    //         return null;
    //     }
    // }

    private void extractPageInformation(WebDriver driver, WebPage page) {
        try {
            // Extract title
            page.setTitle(driver.getTitle());

            // Extract content
            WebElement body = driver.findElement(By.tagName("body"));
            page.setContent(body.getText());

            // Extract metadata
            extractMetadata(driver, page);

            // Extract links
            extractLinks(driver, page);

            // Extract images
            extractImages(driver, page);

            // Calculate metrics
            calculatePageMetrics(driver, page);

        } catch (Exception e) {
            // Log extraction error but continue
            System.err.println("Error extracting page information: " + e.getMessage());
        }
    }

    private void extractMetadata(WebDriver driver, WebPage page) {
        try {
            // Extract meta description
            List<WebElement> metaDescription = driver.findElements(
                By.cssSelector("meta[name='description']"));
            if (!metaDescription.isEmpty()) {
                page.setMetaDescription(metaDescription.get(0).getAttribute("content"));
            }

            // Extract meta keywords
            List<WebElement> metaKeywords = driver.findElements(
                By.cssSelector("meta[name='keywords']"));
            if (!metaKeywords.isEmpty()) {
                page.setMetaKeywords(metaKeywords.get(0).getAttribute("content"));
            }

            // Extract other metadata
            List<WebElement> metaTags = driver.findElements(By.tagName("meta"));
            for (WebElement meta : metaTags) {
                String name = meta.getAttribute("name");
                String content = meta.getAttribute("content");
                if (name != null && content != null) {
                    page.addMetadata(name, content);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting metadata: " + e.getMessage());
        }
    }

    private void extractLinks(WebDriver driver, WebPage page) {
        try {
            List<WebElement> anchors = driver.findElements(By.tagName("a"));
            for (WebElement anchor : anchors) {
                try {
                    String href = anchor.getAttribute("href");
                    if (isValidUrl(href)) {
                        page.addLink(href);
                    }
                } catch (StaleElementReferenceException e) {
                    // Ignore stale elements
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting links: " + e.getMessage());
        }
    }

    private void extractImages(WebDriver driver, WebPage page) {
        try {
            List<WebElement> images = driver.findElements(By.tagName("img"));
            for (WebElement image : images) {
                try {
                    String src = image.getAttribute("src");
                    if (src != null && !src.isEmpty()) {
                        page.addImage(src);
                    }
                } catch (StaleElementReferenceException e) {
                    // Ignore stale elements
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting images: " + e.getMessage());
        }
    }

    private void calculatePageMetrics(WebDriver driver, WebPage page) {
        PageMetrics metrics = page.getMetrics();
        
        // Calculate word and character counts
        String content = page.getContent();
        if (content != null) {
            String[] words = content.split("\\s+");
            metrics.setWordCount(words.length);
            metrics.setCharacterCount(content.length());
        }

        // Set link and image counts
        metrics.setLinkCount(page.getLinks().size());
        metrics.setImageCount(page.getImages().size());

        // Check for various page elements
        try {
            metrics.setHasIframes(!driver.findElements(By.tagName("iframe")).isEmpty());
            metrics.setHasForms(!driver.findElements(By.tagName("form")).isEmpty());
            metrics.setHasJavaScript(detectJavaScript(driver));
        } catch (Exception e) {
            System.err.println("Error calculating metrics: " + e.getMessage());
        }
    }

    private boolean detectJavaScript(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (Boolean) js.executeScript("return window.hasOwnProperty('jQuery')");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && 
               !url.isEmpty() && 
               !url.startsWith("javascript:") && 
               !url.startsWith("mailto:") && 
               !url.startsWith("tel:") && 
               !url.contains("#") &&
               (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void processNewUrls(Set<String> urls) {
        for (String url : urls) {
            if (!session.getVisitedUrls().contains(url)) {
                session.addPendingUrl(url);
            }
        }
    }

    private void handleCrawlError(String url, Exception e) {
        CrawlError error = new CrawlError(url, e.getMessage(), e.getClass().getSimpleName());
        error.setStackTrace(Arrays.toString(e.getStackTrace()));
        
        session.addError(url, error);
        session.getStatistics().incrementFailedPages();
        
        if (e instanceof TimeoutException) {
            session.getStatistics().incrementTimeoutErrors();
        } else {
            session.getStatistics().incrementHttpErrors();
        }
        
        notifyObserversError(url, e);
    }

    public void stopCrawling() {
        isRunning = false;
        session.setEndTime(System.currentTimeMillis());
        executorService.shutdownNow();
        
        // Clean up WebDriver instances
        for (WebDriver driver : drivers.values()) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        drivers.clear();
    }

    public void addObserver(CrawlerObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(WebPage page) {
        for (CrawlerObserver observer : observers) {
            observer.onPageCrawled(page);
        }
    }

    private void notifyObserversError(String url, Exception e) {
        for (CrawlerObserver observer : observers) {
            observer.onError(url, e);
        }
    }

    public CrawlSession getSession() {
        return session;
    }

    // File: src/com/searchengine/core/crawler/WebCrawler.java
// Add these debug methods to your existing WebCrawler class

private void processCrawlRequest(String url) {
    System.out.println("DEBUG: Processing URL: " + url);
    try {
        if (!isRunning) {
            System.out.println("DEBUG: Crawler is not running, skipping URL: " + url);
            return;
        }

        WebDriver driver = getOrCreateDriver();
        System.out.println("DEBUG: Got WebDriver instance");
        
        WebPage page = crawlPage(driver, url);
        System.out.println("DEBUG: Page crawled: " + (page != null ? "success" : "failed"));
        
        if (page != null) {
            System.out.println("DEBUG: Adding URL to visited set: " + url);
            session.addVisitedUrl(url);
            session.incrementPagesProcessed();
            System.out.println("DEBUG: Processed pages count: " + session.getPagesProcessed());
            
            processNewUrls(page.getLinks());
            notifyObservers(page);
        }
    } catch (Exception e) {
        System.err.println("DEBUG: Error processing URL: " + url);
        e.printStackTrace();
        handleCrawlError(url, e);
    }
}

    private WebPage crawlPage(WebDriver driver, String url) {
        System.out.println("DEBUG: Starting to crawl page: " + url);
        try {
            if (cache.hasValidCache(url)) {
                System.out.println("DEBUG: Found cached page for: " + url);
                return cache.getFromCache(url);
            }

            WebPage page = new WebPage();
            page.setUrl(url);

            System.out.println("DEBUG: Navigating to URL");
            driver.get(url);

            System.out.println("DEBUG: Waiting for page load");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(config.getPageLoadTimeout()));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            System.out.println("DEBUG: Page loaded, extracting information");
            extractPageInformation(driver, page);

            cache.addToCache(page);

            return page;
        } catch (Exception e) {
            System.err.println("DEBUG: Error while crawling page: " + url);
            e.printStackTrace();
            return null;
        }
    }
}