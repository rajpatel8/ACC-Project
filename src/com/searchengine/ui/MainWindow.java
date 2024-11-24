package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private final SearchEngine searchEngine;
    private JProgressBar progressBar;
    private JTabbedPane tabbedPane;
    private SearchPanel searchPanel;
    private CrawlerPanel crawlerPanel;
    private AnalysisPanel analysisPanel;

    public MainWindow() {
        searchEngine = new SearchEngine();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Product Search Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create panels
        searchPanel = new SearchPanel(searchEngine);
        crawlerPanel = new CrawlerPanel();
        analysisPanel = new AnalysisPanel(searchEngine);

        // Add panels to tabbed pane
        tabbedPane.addTab("Search", searchPanel);
        tabbedPane.addTab("Crawler", crawlerPanel);
        tabbedPane.addTab("Analysis", analysisPanel);

        // Status bar
        JPanel statusBar = createStatusBar();

        // Add components to frame
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusBar.add(progressBar, BorderLayout.CENTER);
        return statusBar;
    }
}
