// File: src/com/searchengine/ui/CrawlerPanel.java
package com.searchengine.ui;

import com.searchengine.core.cache.PageCache;
import com.searchengine.core.crawler.CrawlerConfig;
import com.searchengine.core.crawler.CrawlerObserver;
import com.searchengine.core.crawler.WebCrawler;
import com.searchengine.model.CrawlSession;
import com.searchengine.model.WebPage;
import javax.swing.*;
import java.awt.*;

public class CrawlerPanel extends JPanel {
    private final String[] WEBSITES = {
            "https://www.bose.ca/en/products/speakers",
            "https://www.sony.ca/en/home-theatre-sound-bars/home-theatre-systems",
            "https://www.lg.com/levant_en/home-theater-system",
            "https://www.samsung.com/us/televisions-home-theater/home-theater/all-home-theater/",
            "https://www.jbl.com/center-channel-speakers/"
    };

    private JTextArea logArea;
    private JComboBox<String> websiteCombo;
    private JButton startButton;
    private WebCrawler crawler;
    private JButton stopButton;
    private volatile boolean isStopping = false;

    public CrawlerPanel() {
        setLayout(new BorderLayout(5, 5));
        initializeUI();
    }

    private void initializeUI() {
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        websiteCombo = new JComboBox<>(WEBSITES);
        startButton = new JButton("Start Crawling");
        startButton.addActionListener(e -> startCrawling());
        stopButton = new JButton("Stop Crawling");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopCrawling());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        controlPanel.add(new JLabel("Select Website:"));
        controlPanel.add(websiteCombo);
        controlPanel.add(startButton);

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    private void startCrawling() {
        String url = (String) websiteCombo.getSelectedItem();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        isStopping = false;
        logArea.setText("");
        addLog("Starting crawler for: " + url);

        CrawlerConfig config = new CrawlerConfig();
        config.setMaxDepth(2);
        config.setMaxPages(20);
        config.setHeadless(true);

        PageCache cache = new PageCache(config.getCacheConfig().getCacheDirectory(),
                config.getCacheConfig().getCacheExpirationHours());

        crawler = new WebCrawler(config, cache);
        crawler.addObserver(new CrawlerObserver() {
            @Override
            public void onPageCrawled(WebPage page) {
                SwingUtilities.invokeLater(() ->
                        addLog("Crawled: " + page.getUrl() + "\nFound " +
                                page.getLinks().size() + " links\n"));
            }

            @Override
            public void onCrawlCompleted(CrawlSession session) {
                SwingUtilities.invokeLater(() -> {
                    addLog("Crawling completed. Processed " + session.getPagesProcessed() + " pages.");
                    crawlingFinished();
                });
            }

            @Override
            public void onError(String url, Exception e) {
                SwingUtilities.invokeLater(() ->
                        addLog("Error crawling " + url + ": " + e.getMessage() + "\n"));
            }
        });

        new Thread(() -> {
            try {
                crawler.startCrawling(url);
                SwingUtilities.invokeLater(this::crawlingFinished);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    addLog("Crawler error: " + e.getMessage() + "\n");
                    crawlingFinished();
                });
            }
        }).start();
    }

    private void stopCrawling() {
        isStopping = true;
    }

    private void crawlingFinished() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void addLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}