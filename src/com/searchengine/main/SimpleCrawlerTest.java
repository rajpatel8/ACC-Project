// File: src/com/searchengine/main/SimpleCrawlerTest.java
package com.searchengine.main;

import com.searchengine.core.crawler.*;
import com.searchengine.model.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleCrawlerTest {
    private static final CountDownLatch completionLatch = new CountDownLatch(1);
    private static final AtomicBoolean crawlingStarted = new AtomicBoolean(false);

    public static void main(String[] args) {
        System.out.println("Starting Crawler Test...\n");
        
        
        
        try {
            // Basic configuration
            CrawlerConfig config = new CrawlerConfig();
            config.setMaxDepth(1);
            config.setMaxPages(20);
            config.setThreadCount(1);
            config.setHeadless(false); // Disable headless mode for debugging
            config.setPageLoadTimeout(10); // Reduce timeout to 10 seconds
            
            // Test ChromeDriver setup first
            if (!testChromeDriver()) {
                System.err.println("ChromeDriver test failed! Exiting...");
                return;
            }
            
            // Create and configure crawler
            WebCrawler crawler = new WebCrawler(config);
            final WebCrawler finalCrawler = crawler;
            
            // Add observer for monitoring
            crawler.addObserver(new CrawlerObserver() {
                private int pageCount = 0;

                @Override
                public void onCrawlStarted(String seedUrl) {
                    crawlingStarted.set(true);
                    System.out.println("\n🚀 Crawl Started");
                    System.out.println("Seed URL: " + seedUrl);
                    System.out.println("Configuration:");
                    System.out.println("- Max Depth: " + config.getMaxDepth());
                    System.out.println("- Max Pages: " + config.getMaxPages());
                    System.out.println("- Threads: " + config.getThreadCount());
                    System.out.println("- Headless Mode: " + config.isHeadless());
                    System.out.println("-----------------------------------------");
                }

                @Override
                public void onPageCrawled(WebPage page) {
                    pageCount++;
                    System.out.println("\n✅ Page " + pageCount + " Crawled Successfully");
                    System.out.println("URL: " + page.getUrl());
                    System.out.println("Title: " + page.getTitle());
                    
                    if (pageCount >= config.getMaxPages()) {
                        System.out.println("\nReached max pages limit. Stopping crawler...");
                        finalCrawler.stopCrawling();
                        completionLatch.countDown();
                    }
                }

                @Override
                public void onError(String url, Exception e) {
                    System.err.println("\n❌ Error Crawling URL: " + url);
                    System.err.println("Error Type: " + e.getClass().getSimpleName());
                    System.err.println("Error Message: " + e.getMessage());
                    e.printStackTrace();
                }

                @Override
                public void onCrawlCompleted(CrawlSession session) {
                    System.out.println("\n🏁 Crawl Completed!");
                    System.out.println("Pages Processed: " + session.getPagesProcessed());
                    completionLatch.countDown();
                }
            });

            // Start crawler in a separate thread
            Thread crawlerThread = new Thread(() -> {
                try {
                    System.out.println("Starting crawler thread...");
                    crawler.startCrawling("https://www.bose.com/home");
                } catch (Exception e) {
                    System.err.println("Error in crawler thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Start the crawler thread
            System.out.println("Initializing crawler...");
            crawlerThread.start();

            // Wait for crawling to start
            System.out.println("Waiting for crawling to start...");
            long startTime = System.currentTimeMillis();
            while (!crawlingStarted.get()) {
                if (System.currentTimeMillis() - startTime > 30000) { // 30 seconds timeout
                    System.err.println("Crawler failed to start within 30 seconds!");
                    if (crawler != null) {
                        crawler.stopCrawling();
                    }
                    return;
                }
                Thread.sleep(1000);
                System.out.println("Still waiting for crawler to start...");
            }

            // Wait for completion or timeout
            if (!completionLatch.await(2, TimeUnit.MINUTES)) {
                System.err.println("Crawler did not complete within 2 minutes!");
                if (crawler != null) {
                    crawler.stopCrawling();
                }
            }

        } catch (Exception e) {
            System.err.println("\n💥 Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure crawler is stopped
            // if (crawler != null) {
            //     crawler.stopCrawling();
            // }
        }
    }
    
    private static boolean testChromeDriver() {
        System.out.println("Testing ChromeDriver setup...");
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage"
            );
            
            driver = new ChromeDriver(options);
            driver.get("https://www.wikipedia.org");
            String title = driver.getTitle();
            System.out.println("ChromeDriver test successful! ✅");
            System.out.println("Page Title: " + title);
            return true;
            
        } catch (Exception e) {
            System.err.println("ChromeDriver test failed! ❌");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}