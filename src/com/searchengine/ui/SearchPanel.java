package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.core.SearchResult;
import com.searchengine.core.completion.WordCompletion;
import com.searchengine.model.Product;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class SearchPanel extends JPanel {
    private final SearchEngine searchEngine;
    private final WordCompletion wordCompletion;
    private JTextField searchField;
    private JTextArea resultArea;
    private JList<String> suggestionsList;
    private DefaultListModel<String> suggestionsModel;
    private JWindow suggestionsWindow;
    private List<Product> products;

    public SearchPanel(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        this.wordCompletion = new WordCompletion();
        this.products = new ArrayList<>();
        initializeUI();
        loadProductData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search panel with autocomplete
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());

        // Setup suggestions
        suggestionsModel = new DefaultListModel<>();
        suggestionsList = new JList<>(suggestionsModel);
        suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create suggestions window
        suggestionsWindow = new JWindow(SwingUtilities.getWindowAncestor(this));
        JScrollPane suggestionsScroll = new JScrollPane(suggestionsList);
        suggestionsScroll.setPreferredSize(new Dimension(300, 200));
        suggestionsWindow.add(suggestionsScroll);
        suggestionsWindow.setSize(300, 200);

        // Add document listener for search field
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
        });

        // Handle key events
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestionsList.isShowing()) {
                    suggestionsList.setSelectedIndex(0);
                    suggestionsList.requestFocus();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    suggestionsWindow.setVisible(false);
                    performSearch();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestionsWindow.setVisible(false);
                }
            }
        });

        // Add selection listener for suggestions
        suggestionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && suggestionsList.getSelectedValue() != null) {
                searchField.setText(suggestionsList.getSelectedValue());
                suggestionsWindow.setVisible(false);
                searchField.requestFocus();
            }
        });

        // Add mouse listener for suggestions
        suggestionsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = suggestionsList.getSelectedValue();
                if (selected != null) {
                    searchField.setText(selected);
                    suggestionsWindow.setVisible(false);
                    searchField.requestFocus();
                    performSearch();
                }
            }
        });

        // Add focus listener to hide suggestions
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Don't hide if focus is going to suggestions list
                if (e.getOppositeComponent() != suggestionsList) {
                    Timer timer = new Timer(200, evt -> {
                        if (!suggestionsList.hasFocus()) {
                            suggestionsWindow.setVisible(false);
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        JScrollPane resultScroll = new JScrollPane(resultArea);

        add(searchPanel, BorderLayout.NORTH);
        add(resultScroll, BorderLayout.CENTER);
    }


    private void showSuggestions(String prefix) {
        List<WordCompletion.Suggestion> suggestions = wordCompletion.getSuggestions(prefix);

        suggestionsModel.clear();
        if (!suggestions.isEmpty()) {
            suggestions.forEach(suggestion -> suggestionsModel.addElement(suggestion.getWord()));

            // Position and show suggestions window
            Point p = searchField.getLocationOnScreen();
            suggestionsWindow.setLocation(p.x, p.y + searchField.getHeight());
            suggestionsWindow.setVisible(true);
        } else {
            suggestionsWindow.setVisible(false);
        }
    }
    private void loadProductData() {
        try {
            // Read the JSON file
            String jsonContent = new String(Files.readAllBytes(Paths.get("audio_products.json")));

            // Parse JSON
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            JsonObject companies = root.getAsJsonObject("company");

            // Process each company's products
            for (Map.Entry<String, JsonElement> company : companies.entrySet()) {
                String companyName = company.getKey();
                JsonObject companyData = company.getValue().getAsJsonObject();

                for (JsonElement productElement : companyData.getAsJsonArray("products")) {
                    JsonObject productJson = productElement.getAsJsonObject();

                    Product product = new Product();
                    product.setName(productJson.get("name").getAsString());
                    product.setPrice(productJson.get("price").getAsDouble());
                    product.setCategory(companyName);

                    // Add audio technologies as features
                    for (JsonElement tech : productJson.getAsJsonArray("audioTechnology")) {
                        product.addFeature(tech.getAsString());
                    }

                    // Add connectivity options as features
                    for (JsonElement conn : productJson.getAsJsonArray("connectivity")) {
                        product.addFeature("Connectivity: " + conn.getAsString());
                    }

                    // Add specifications
                    product.addSpecification("Speaker Configuration",
                            productJson.get("speakerConfiguration").getAsString());
                    product.addSpecification("Power Output",
                            productJson.get("powerOutput").getAsString());
                    product.addSpecification("Type",
                            productJson.get("type").getAsString());

                    products.add(product);
                }
            }

            // Initialize word completion
            wordCompletion.buildTrie(products);
            System.out.println("Loaded " + products.size() + " products");

        } catch (Exception e) {
            System.err.println("Error loading product data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSuggestions() {
        String text = searchField.getText().trim();
        if (text.length() >= 2) {
            SwingUtilities.invokeLater(() -> showSuggestions(text));
        } else {
            suggestionsWindow.setVisible(false);
        }
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

        List<Product> results = searchProducts(query);
        displayResults(results);
    }

    private List<Product> searchProducts(String query) {
        String[] terms = query.toLowerCase().split("\\s+");

        return products.stream()
                .filter(product -> {
                    String searchText = (product.getName() + " " +
                            product.getCategory() + " " +
                            String.join(" ", product.getFeatures()) + " " +
                            String.join(" ", product.getSpecifications().values()))
                            .toLowerCase();

                    return Arrays.stream(terms)
                            .allMatch(term -> searchText.contains(term));
                })
                .sorted((p1, p2) -> {
                    int score1 = calculateRelevanceScore(p1, query);
                    int score2 = calculateRelevanceScore(p2, query);
                    return Integer.compare(score2, score1);
                })
                .toList();
    }

    private int calculateRelevanceScore(Product product, String query) {
        int score = 0;
        String[] terms = query.toLowerCase().split("\\s+");

        for (String term : terms) {
            // Name matches (highest weight)
            if (product.getName().toLowerCase().contains(term)) score += 3;

            // Category matches (medium weight)
            if (product.getCategory().toLowerCase().contains(term)) score += 2;

            // Feature matches (medium weight)
            for (String feature : product.getFeatures()) {
                if (feature.toLowerCase().contains(term)) score += 2;
            }

            // Specification matches (lower weight)
            for (String spec : product.getSpecifications().values()) {
                if (spec.toLowerCase().contains(term)) score += 1;
            }
        }

        return score;
    }

    private void displayResults(List<Product> results) {
        StringBuilder sb = new StringBuilder();

        if (results.isEmpty()) {
            sb.append("No products found matching your search.\n");
        } else {
            sb.append("Found ").append(results.size()).append(" products:\n\n");

            for (Product product : results) {
                sb.append("Name: ").append(product.getName()).append("\n");
                sb.append("Company: ").append(product.getCategory()).append("\n");
                sb.append("Price: $").append(String.format("%.2f", product.getPrice())).append("\n");
                sb.append("Type: ").append(product.getSpecifications().get("Type")).append("\n");
                sb.append("Features:\n");
                for (String feature : product.getFeatures()) {
                    sb.append("  - ").append(feature).append("\n");
                }
                sb.append("Specifications:\n");
                for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                    sb.append("  - ").append(spec.getKey()).append(": ")
                            .append(spec.getValue()).append("\n");
                }
                sb.append("\n");
            }
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }
}