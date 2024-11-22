package com.searchengine.main;

import com.searchengine.core.crawler.*;
import com.searchengine.core.cache.*;
import com.searchengine.model.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Duration;

public class SimpleCrawlerTest {
    private static final CountDownLatch completionLatch = new CountDownLatch(1);
    private static final AtomicBoolean crawlingStarted = new AtomicBoolean(false);
    private static final String TARGET_URL = "https://www.bose.ca/en/c/home-theater";

    public static void main(String[] args) {
        System.out.println("Starting Crawler Test...\n");

        WebCrawler crawler = null;

        try {
            // Basic configuration
            CrawlerConfig config = new CrawlerConfig();
            config.setMaxDepth(1);
            config.setMaxPages(20);
            config.setThreadCount(1);
            config.setHeadless(false);
            config.setPageLoadTimeout(10);

            // Configure cache
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.setCacheDirectory("bose_cache");
            cacheConfig.setCacheExpirationHours(24);
            cacheConfig.setCacheEnabled(true);
            config.setCacheConfig(cacheConfig);

            // Test ChromeDriver setup first
            if (!testChromeDriver()) {
                System.err.println("ChromeDriver test failed! Exiting...");
                return;
            }

            // First crawl
            System.out.println("\nStarting first crawl (without cache):");
            runCrawlTest(config);

            Thread.sleep(2000); // Short pause between crawls

            // Second crawl (should use cache)
            System.out.println("\nStarting second crawl (should use cache):");
            runCrawlTest(config);

        } catch (Exception e) {
            System.err.println("\n💥 Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runCrawlTest(CrawlerConfig config) throws InterruptedException {
        WebCrawler crawler = new WebCrawler(config);
        CountDownLatch testCompletionLatch = new CountDownLatch(1);
        AtomicBoolean testCrawlingStarted = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        crawler.addObserver(new CrawlerObserver() {
            private int pageCount = 0;
            private long crawlStartTime;

            @Override
            public void onCrawlStarted(String seedUrl) {
                testCrawlingStarted.set(true);
                crawlStartTime = System.currentTimeMillis();
                System.out.println("\n🚀 Crawl Started");
                System.out.println("Seed URL: " + seedUrl);
                System.out.println("Configuration:");
                System.out.println("- Max Depth: " + config.getMaxDepth());
                System.out.println("- Max Pages: " + config.getMaxPages());
                System.out.println("- Cache Enabled: " + config.getCacheConfig().isCacheEnabled());
                System.out.println("-----------------------------------------");
            }

            @Override
            public void onPageCrawled(WebPage page) {
                pageCount++;
                System.out.println("\n✅ Page " + pageCount + " Processed");
                System.out.println("URL: " + page.getUrl());
                System.out.println("Title: " + page.getTitle());
                System.out.println("Processing Time: " + page.getMetrics().getLoadTime() + "ms");
                System.out.println("Word Count: " + page.getMetrics().getWordCount());

                // Print some of the found links
                if (!page.getLinks().isEmpty()) {
                    System.out.println("\nFound Links (first 5):");
                    page.getLinks().stream()
                            .limit(5)
                            .forEach(link -> System.out.println("- " + link));
                }

                if (pageCount >= config.getMaxPages()) {
                    System.out.println("\nReached max pages limit. Stopping crawler...");
                    crawler.stopCrawling();
                    testCompletionLatch.countDown();
                }
            }

            @Override
            public void onError(String url, Exception e) {
                System.err.println("\n❌ Error Processing URL: " + url);
                System.err.println("Error Type: " + e.getClass().getSimpleName());
                System.err.println("Error Message: " + e.getMessage());
            }

            @Override
            public void onCrawlCompleted(CrawlSession session) {
                long duration = System.currentTimeMillis() - crawlStartTime;
                System.out.println("\n🏁 Crawl Completed!");
                System.out.println("Summary:");
                System.out.println("- Pages Processed: " + session.getPagesProcessed());
                System.out.println("- Total Time: " + duration + "ms");
                System.out.println("- Average Time Per Page: " +
                        (session.getPagesProcessed() > 0 ? duration / session.getPagesProcessed() : 0) + "ms");
                System.out.println("- Successful Pages: " + session.getStatistics().getSuccessfulPages());
                System.out.println("- Failed Pages: " + session.getStatistics().getFailedPages());
                testCompletionLatch.countDown();
            }
        });

        // Start crawler in a separate thread
        Thread crawlerThread = new Thread(() -> {
            try {
                crawler.startCrawling(TARGET_URL);
            } catch (Exception e) {
                System.err.println("Error in crawler thread: " + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("Starting crawler thread...");
        crawlerThread.start();

        // Wait for completion or timeout
        if (!testCompletionLatch.await(5, TimeUnit.MINUTES)) {
            System.err.println("Crawler did not complete within 5 minutes!");
            crawler.stopCrawling();
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
                    "--disable-dev-shm-usage",
                    "--disable-extensions",
                    "--disable-popup-blocking"
            );

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(TARGET_URL);
            String title = driver.getTitle();
            System.out.println("ChromeDriver test successful! ✅");
            System.out.println("Page Title: " + title);
            return true;

        } catch (Exception e) {
            System.err.println("ChromeDriver test failed! ❌");
            System.err.println("Error: " + e.getMessage());
            return false;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}