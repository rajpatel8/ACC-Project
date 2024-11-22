package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.core.SearchResult;
import javax.swing.*;
import java.awt.*;

public class SearchPanel extends JPanel {
    private final SearchEngine searchEngine;
    private JTextField searchField;
    private JTextArea resultArea;
    private JList<String> suggestionsList;

    public SearchPanel(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Results split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultArea);

        // Suggestions list
        suggestionsList = new JList<>();
        JScrollPane suggestionsScroll = new JScrollPane(suggestionsList);

        splitPane.setLeftComponent(resultScroll);
        splitPane.setRightComponent(suggestionsScroll);
        splitPane.setResizeWeight(0.7);

        add(searchPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a search query",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingWorker<SearchResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SearchResult doInBackground() {
                return searchEngine.search(query);
            }

            @Override
            protected void done() {
                try {
                    SearchResult result = get();
                    updateResults(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SearchPanel.this,
                            "Error performing search: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateResults(SearchResult result) {
        StringBuilder sb = new StringBuilder();

        // Add spell check suggestions
        if (!result.getSpellingSuggestions().isEmpty()) {
            sb.append("Did you mean: ")
                    .append(String.join(", ", result.getSpellingSuggestions()))
                    .append("\n\n");
        }

        // Add search results
        if (result.getRankedResults().isEmpty()) {
            sb.append("No results found\n");
        } else {
            sb.append("Search Results:\n\n");
            result.getRankedResults().forEach(product -> {
                sb.append(product.toString()).append("\n");
                sb.append("Score: ").append(product.getScore()).append("\n\n");
            });
        }

        resultArea.setText(sb.toString());

        // Update suggestions list
        DefaultListModel<String> model = new DefaultListModel<>();
        result.getCompletions().forEach(suggestion ->
                model.addElement(suggestion.toString()));
        suggestionsList.setModel(model);
    }
}