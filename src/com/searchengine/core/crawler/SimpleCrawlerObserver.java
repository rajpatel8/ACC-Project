package com.searchengine.core.crawler;

import com.searchengine.model.WebPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimpleCrawlerObserver implements CrawlerObserver {
    @Override
    public void onPageCrawled(WebPage page) {
        System.out.println("Page crawled: " + page.getUrl());
        storeCrawledPage(page);
    }

    private void storeCrawledPage(WebPage page) {
        String directory = "crawled_pages";
        String filename = page.getUrl().replaceAll("[^a-zA-Z0-9]", "_") + ".html";
        Path filePath = Paths.get(directory, filename);

        try {
            Files.createDirectories(Paths.get(directory));
            Files.writeString(filePath, page.getContent());
        } catch (IOException e) {
            System.err.println("Error storing crawled page: " + page.getUrl());
            e.printStackTrace();
        }
    }

    @Override
    public void onError(String url, Exception e) {
        System.err.println("Error crawling URL: " + url);
        e.printStackTrace();
    }
}