package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
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
        // Clear previous results
        resultArea.removeAll();

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);

        if (results.isEmpty()) {
            JLabel noResultsLabel = new JLabel("No products found matching your search.");
            noResultsLabel.setForeground(Color.RED);
            noResultsLabel.setFont(new Font("Arial", Font.BOLD, 18));
            noResultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            resultsPanel.add(noResultsLabel);
        } else {
            JLabel resultsHeader = new JLabel("🔍 Found " + results.size() + " products:");
            resultsHeader.setFont(new Font("Arial", Font.BOLD, 20));
            resultsHeader.setForeground(new Color(0, 123, 255)); // Blue color
            resultsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            resultsPanel.add(resultsHeader);
            resultsPanel.add(Box.createVerticalStrut(15)); // Add space below header

            for (Product product : results) {
                JPanel productPanel = new JPanel();
                productPanel.setLayout(new BorderLayout());
                productPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200), 2, true),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10) // Add padding
                ));
                productPanel.setBackground(new Color(248, 249, 250)); // Light gray
                productPanel.setMaximumSize(new Dimension(600, 250));
                productPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Product Image
                JLabel imageLabel = new JLabel();
                ImageIcon productImage = new ImageIcon("/Users/lord_rajkumar/Desktop/Project/ACC/src/stock.jpg"); // Path to the default image
                Image scaledImage = productImage.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // Text Information Panel
                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setBackground(new Color(248, 249, 250));
                infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding inside info panel

                // Product Name
                JLabel nameLabel = new JLabel("📦 " + product.getName());
                nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
                nameLabel.setForeground(new Color(52, 58, 64));

                // Product Category
                JLabel companyLabel = new JLabel("🏢 Company: " + product.getCategory());
                companyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                companyLabel.setForeground(new Color(108, 117, 125));

                // Product Price
                JLabel priceLabel = new JLabel("💲 Price: $" + String.format("%.2f", product.getPrice()));
                priceLabel.setFont(new Font("Arial", Font.BOLD, 16));
                priceLabel.setForeground(new Color(40, 167, 69)); // Green

                // Product Type
                JLabel typeLabel = new JLabel("🛠️ Type: " + product.getSpecifications().get("Type"));
                typeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                typeLabel.setForeground(new Color(73, 80, 87));

                // Features
                JPanel featuresPanel = new JPanel();
                featuresPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                featuresPanel.setBackground(new Color(248, 249, 250));
                for (String feature : product.getFeatures()) {
                    JLabel featureLabel = new JLabel("✔️ " + feature);
                    featureLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                    featureLabel.setForeground(Color.DARK_GRAY);
                    featuresPanel.add(featureLabel);
                }

                // Add elements to the info panel
                infoPanel.add(nameLabel);
                infoPanel.add(Box.createVerticalStrut(5));
                infoPanel.add(companyLabel);
                infoPanel.add(priceLabel);
                infoPanel.add(typeLabel);
                infoPanel.add(featuresPanel);

                // Add Image and Info Panel to Product Panel
                productPanel.add(imageLabel, BorderLayout.WEST);
                productPanel.add(infoPanel, BorderLayout.CENTER);

                // Add space between products
                resultsPanel.add(productPanel);
                resultsPanel.add(Box.createVerticalStrut(15));
            }
        }

        // Add results to the result area
        resultArea.setLayout(new BorderLayout());
        resultArea.add(new JScrollPane(resultsPanel), BorderLayout.CENTER);

        // Refresh the UI
        resultArea.revalidate();
        resultArea.repaint();
    }
}