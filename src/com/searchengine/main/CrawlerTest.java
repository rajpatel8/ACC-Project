
// File: src/com/searchengine/main/CrawlerTest.java
package com.searchengine.main;

import com.searchengine.core.crawler.*;
import com.searchengine.model.CrawlSession;
import com.searchengine.model.WebPage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrawlerTest {
    public static void main(String[] args) {
        // Create crawler configuration
        CrawlerConfig config = new CrawlerConfig();
        config.setMaxDepth(2);
        config.setMaxPages(10);
        config.setThreadCount(4);
        config.setDriverPath("/opt/homebrew/bin/chromedriver"); // Update with your path
        config.setHeadless(true);
        config.addAllowedDomain("example.com");
        
        // Create output directory for crawled pages
        File outputDir = new File("crawled_pages");
        outputDir.mkdir();
        
        // Create crawler instance
        WebCrawler crawler = new WebCrawler(config);

        // Add observer to handle crawled pages
        crawler.addObserver(new CrawlerObserver() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            private int pageCount = 0;

            @Override
            public void onCrawlStarted(String seedUrl) {
                System.out.println("Starting crawl from: " + seedUrl);
                System.out.println("Timestamp: " + new Date());
                System.out.println("----------------------------------------");
            }

            @Override
            public void onPageCrawled(WebPage page) {
                pageCount++;
                System.out.println("Page " + pageCount + " crawled: " + page.getUrl());
                System.out.println("Title: " + page.getTitle());
                System.out.println("Links found: " + page.getLinks().size());
                System.out.println("Images found: " + page.getImages().size());
                System.out.println("Word count: " + page.getMetrics().getWordCount());
                System.out.println("Load time: " + page.getMetrics().getLoadTime() + "ms");
                System.out.println("----------------------------------------");

                // Save page content to file
                try {
                    String filename = dateFormat.format(new Date()) + "_" + 
                                   pageCount + ".html";
                    File outputFile = new File(outputDir, filename);
                    
                    try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                        writer.println("URL: " + page.getUrl());
                        writer.println("Title: " + page.getTitle());
                        writer.println("Crawl Time: " + new Date(page.getCrawlTimestamp()));
                        writer.println("----------------------------------------");
                        writer.println(page.getContent());
                    }
                } catch (Exception e) {
                    System.err.println("Error saving page: " + e.getMessage());
                }
            }

            @Override
            public void onError(String url, Exception e) {
                System.err.println("Error crawling: " + url);
                System.err.println("Error type: " + e.getClass().getSimpleName());
                System.err.println("Error message: " + e.getMessage());
                System.err.println("----------------------------------------");
            }

            @Override
            public void onCrawlCompleted(CrawlSession session) {
                System.out.println("\nCrawl Summary:");
                System.out.println("Total pages processed: " + session.getPagesProcessed());
                System.out.println("Successful pages: " + session.getStatistics().getSuccessfulPages());
                System.out.println("Failed pages: " + session.getStatistics().getFailedPages());
                System.out.println("Total time: " + (session.getEndTime() - session.getStartTime()) + "ms");
                System.out.println("Average time per page: " + session.getStatistics().getAveragePageTime() + "ms");
                System.out.println("----------------------------------------");
            }

            @Override
            public void onCrawlStatusUpdate(CrawlSession session) {
                System.out.println("Pages processed: " + session.getPagesProcessed());
                System.out.println("Pending URLs: " + session.getPendingUrls().size());
                System.out.println("----------------------------------------");
            }
        });

        // Start crawling
        try {
            System.out.println("Initializing crawler...");
            crawler.startCrawling("https://example.com");
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}