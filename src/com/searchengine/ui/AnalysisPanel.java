// File: src/com/searchengine/ui/AnalysisPanel.java
package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.core.frequency.*;
import com.searchengine.core.patterns.*;
import com.searchengine.model.Product;
import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class AnalysisPanel extends JPanel {
    private final SearchEngine searchEngine;
    private JTabbedPane tabbedPane;
    private JTable frequencyTable;
    private JTable patternTable;
    private JTextArea statsArea;
    private DefaultTableModel frequencyModel;
    private DefaultTableModel patternModel;

    public AnalysisPanel(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Word Frequencies", createFrequencyPanel());
        tabbedPane.addTab("Patterns", createPatternPanel());
        tabbedPane.addTab("Statistics", createStatsPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.NORTH);
    }

    private JPanel createFrequencyPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Word", "Frequency", "Products"};
        frequencyModel = new DefaultTableModel(columns, 0);
        frequencyTable = new JTable(frequencyModel);

        JScrollPane scrollPane = new JScrollPane(frequencyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Pattern Type", "Found Text", "Location", "Count"};
        patternModel = new DefaultTableModel(columns, 0);
        patternTable = new JTable(patternModel);

        JScrollPane scrollPane = new JScrollPane(patternTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        statsArea = new JTextArea();
        statsArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(statsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton analyzeButton = new JButton("Analyze Products");
        analyzeButton.addActionListener(e -> analyzeProducts());

        JButton exportButton = new JButton("Export Results");
        exportButton.addActionListener(e -> exportResults());

        panel.add(analyzeButton);
        panel.add(exportButton);

        return panel;
    }

    private void analyzeProducts() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                updateFrequencyTable();
                updatePatternTable();
                updateStats();
                return null;
            }
        };
        worker.execute();
    }

    private void updateFrequencyTable() {
        FrequencyAnalyzer analyzer = searchEngine.getFrequencyAnalyzer();
        frequencyModel.setRowCount(0);

        for (FrequencyResult result : analyzer.getTopWords(100)) {
            frequencyModel.addRow(new Object[]{
                    result.getWord(),
                    result.getFrequency(),
                    String.join(", ", getProductsForWord(result.getWord()))
            });
        }
    }

    private void updatePatternTable() {
        PatternMatcher matcher = searchEngine.getPatternMatcher();
        patternModel.setRowCount(0);

        Map<String, Integer> patternCounts = new HashMap<>();
        for (Product product : searchEngine.getProducts()) {
            List<PatternMatch> matches = matcher.findPatterns(product);
            for (PatternMatch match : matches) {
                String key = match.getFeatureType() + "|" + match.getMatchedText();
                patternCounts.merge(key, 1, Integer::sum);

                patternModel.addRow(new Object[]{
                        match.getFeatureType(),
                        match.getMatchedText(),
                        match.getField(),
                        patternCounts.get(key)
                });
            }
        }
    }

    private void updateStats() {
        FrequencyAnalyzer analyzer = searchEngine.getFrequencyAnalyzer();
        StringBuilder stats = new StringBuilder();

        stats.append("Analysis Statistics:\n\n");
        stats.append(String.format("Total Products: %d\n", searchEngine.getProducts().size()));
        stats.append(String.format("Unique Words: %d\n", analyzer.getTopWords(Integer.MAX_VALUE).size()));
        stats.append(String.format("Average Words per Product: %.2f\n", calculateAverageWordsPerProduct()));
        stats.append("\nTop Searches:\n");

        analyzer.getTopSearches(10).forEach(term ->
                stats.append(String.format("- %s (%d times)\n", term.getTerm(), term.getCount())));

        statsArea.setText(stats.toString());
    }

    private List<String> getProductsForWord(String word) {
        List<String> products = new ArrayList<>();
        for (Product product : searchEngine.getProducts()) {
            if (product.getName().toLowerCase().contains(word.toLowerCase()) ||
                    product.getDescription().toLowerCase().contains(word.toLowerCase())) {
                products.add(product.getName());
            }
        }
        return products;
    }

    private double calculateAverageWordsPerProduct() {
        int totalWords = 0;
        for (Product product : searchEngine.getProducts()) {
            totalWords += product.getName().split("\\s+").length;
            if (product.getDescription() != null) {
                totalWords += product.getDescription().split("\\s+").length;
            }
        }
        return (double) totalWords / searchEngine.getProducts().size();
    }

    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                StringBuilder export = new StringBuilder();

                // Export frequency data
                export.append("Word Frequencies:\n");
                for (int i = 0; i < frequencyModel.getRowCount(); i++) {
                    export.append(String.format("%s,%d,%s\n",
                            frequencyModel.getValueAt(i, 0),
                            frequencyModel.getValueAt(i, 1),
                            frequencyModel.getValueAt(i, 2)));
                }

                // Export pattern data
                export.append("\nPatterns:\n");
                for (int i = 0; i < patternModel.getRowCount(); i++) {
                    export.append(String.format("%s,%s,%s,%d\n",
                            patternModel.getValueAt(i, 0),
                            patternModel.getValueAt(i, 1),
                            patternModel.getValueAt(i, 2),
                            patternModel.getValueAt(i, 3)));
                }

                // Export statistics
                export.append("\nStatistics:\n");
                export.append(statsArea.getText());

                Files.write(fileChooser.getSelectedFile().toPath(),
                        export.toString().getBytes());

                JOptionPane.showMessageDialog(this,
                        "Results exported successfully",
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting results: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}