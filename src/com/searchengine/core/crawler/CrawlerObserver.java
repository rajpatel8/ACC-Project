package com.searchengine.core.crawler;

import com.searchengine.model.CrawlSession;
import com.searchengine.model.WebPage;

public interface CrawlerObserver {
    void onPageCrawled(WebPage page);
    void onError(String url, Exception e);
    default void onCrawlStarted(String seedUrl) {}
    default void onCrawlCompleted(CrawlSession session) {}
    default void onCrawlStatusUpdate(CrawlSession session) {}
}
