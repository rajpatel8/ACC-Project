// File: src/com/searchengine/main/ProductCrawlerTest.java
package com.searchengine.main;

import com.searchengine.core.crawler.*;
import com.searchengine.model.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.*;

public class ProductCrawlerTest {
    private static final String[] TARGET_URLS = {
            "https://www.bose.ca/en/c/home-theater"
//            "https://www.bose.ca/en/products/headphones"
    };

    public static void main(String[] args) {
        System.out.println("Starting Product Crawler Test...\n");

        try {
            // Configure crawler
            CrawlerConfig config = new CrawlerConfig();
            config.setMaxDepth(2);
            config.setMaxPages(50);
            config.setThreadCount(1);
            config.setHeadless(false);
            config.setPageLoadTimeout(15);
            config.setOutputDirectory("bose_products");
            config.setExtractProducts(true);
            config.setSaveImages(true);

            // Add allowed categories
//            config.addAllowedProductCategory("speakers");
            config.addAllowedProductCategory("home_theater");
//            config.addAllowedProductCategory("headphones");

            // Create output directory
            Files.createDirectories(Paths.get(config.getOutputDirectory()));

            // Crawl each target URL
            for (String url : TARGET_URLS) {
                System.out.println("\nCrawling category: " + url);
                crawlCategory(config, url);
                Thread.sleep(2000); // Pause between categories
            }

        } catch (Exception e) {
            System.err.println("\n💥 Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void crawlCategory(CrawlerConfig config, String url)
            throws InterruptedException {

        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicBoolean crawlingStarted = new AtomicBoolean(false);
        WebCrawler crawler = new WebCrawler(config);

        crawler.addObserver(new CrawlerObserver() {
            private int productCount = 0;

            @Override
            public void onCrawlStarted(String seedUrl) {
                crawlingStarted.set(true);
                System.out.println("\n🚀 Starting category crawl");
                System.out.println("URL: " + seedUrl);
            }

            @Override
            public void onPageCrawled(WebPage page) {
                if (page instanceof ProductPage) {
                    ProductPage productPage = (ProductPage) page;

                    // Print product information
                    for (Product product : productPage.getProducts()) {
                        productCount++;
                        System.out.println("\n📦 Product " + productCount + ":");
                        System.out.println("Name: " + product.getName());
                        System.out.println("Price: $" + product.getPrice());
                        if (!product.getFeatures().isEmpty()) {
                            System.out.println("Features:");
                            product.getFeatures().forEach(f ->
                                    System.out.println("  - " + f));
                        }
                        System.out.println("URL: " + product.getUrl());
                    }

                    // Print page information
                    System.out.println("\nPage Type: " + productPage.getPageType());
                    System.out.println("Products found on page: " +
                            productPage.getProducts().size());
                    System.out.println("Total products so far: " + productCount);
                }
            }

            @Override
            public void onError(String url, Exception e) {
                System.err.println("\n❌ Error processing: " + url);
                System.err.println("Error: " + e.getMessage());
            }

            @Override
            public void onCrawlCompleted(CrawlSession session) {
                System.out.println("\n🏁 Category crawl completed!");
                System.out.println("Total pages processed: " +
                        session.getPagesProcessed());
                System.out.println("Total products found: " + productCount);
                System.out.println("Time taken: " + session.getDuration() + "ms");
                completionLatch.countDown();
            }
        });

        // Start crawling
        new Thread(() -> crawler.startCrawling(url)).start();

        // Wait for completion or timeout
        if (!completionLatch.await(10, TimeUnit.MINUTES)) {
            System.err.println("Crawler timed out for category: " + url);
            crawler.stopCrawling();
        }
    }
}