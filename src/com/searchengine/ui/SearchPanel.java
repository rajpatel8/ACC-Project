package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.core.completion.WordCompletion;
import com.searchengine.model.Product;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SearchPanel extends JPanel {
    private final SearchEngine searchEngine;
    private final WordCompletion wordCompletion;
    private JTextField searchField;
    private JTextArea resultArea;
    private JList<String> suggestionsList;
    private DefaultListModel<String> suggestionsModel;
    private JWindow suggestionsWindow;
    private List<Product> products;
    private JLabel spellCheckLabel;
    private Timer spellCheckTimer;
    private static final int SPELL_CHECK_DELAY = 500;
    private JList<SearchHistoryEntry> historyList;
    private DefaultListModel<SearchHistoryEntry> historyModel;
    private Map<String, SearchHistoryEntry> searchHistory; // Changed to Map for quick lookup
    private DefaultTableModel frequencyModel;
    private JTable frequencyTable;

    private void addHistoryPanel() {
        // Create history panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setPreferredSize(new Dimension(250, getHeight()));

        // Create tabbed pane for history and frequency
        JTabbedPane tabbedPane = new JTabbedPane();

        // Initialize history components
        searchHistory = new HashMap<>();
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);

        // Custom cell renderer for history items
        historyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                SearchHistoryEntry entry = (SearchHistoryEntry) value;
                String displayText = String.format("<html><b>%s</b><br>" +
                                "<small>Last: %s</small><br>" +
                                "<small>Frequency: %d times</small></html>",
                        entry.getQuery(),
                        entry.getLastSearchTime(),
                        entry.getFrequency());

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, displayText, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return label;
            }
        });

        // Add mouse listener for history items
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {  // Double click
                    SearchHistoryEntry selected = historyList.getSelectedValue();
                    if (selected != null) {
                        searchField.setText(selected.getQuery());
                        performSearch();
                    }
                }
            }
        });

        // Create history control panel
        JPanel historyControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("Clear History");
        clearButton.addActionListener(e -> clearHistory());
        historyControlPanel.add(clearButton);

        // Create history tab
        JPanel historyTab = new JPanel(new BorderLayout());
        historyTab.add(new JScrollPane(historyList), BorderLayout.CENTER);
        historyTab.add(historyControlPanel, BorderLayout.SOUTH);

        // Create frequency table
        String[] columns = {"Search Term", "Frequency", "Last Search"};
        frequencyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        frequencyTable = new JTable(frequencyModel);
        frequencyTable.setFillsViewportHeight(true);

        // Set column widths
        frequencyTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        frequencyTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        frequencyTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        // Sort by frequency
        frequencyTable.setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(frequencyTable.getModel());
        frequencyTable.setRowSorter(sorter);
        // Sort by frequency (column 1) in descending order by default
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);

        // Create frequency tab
        JPanel frequencyTab = new JPanel(new BorderLayout());
        frequencyTab.add(new JScrollPane(frequencyTable), BorderLayout.CENTER);

        // Add tabs to tabbed pane
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Frequency", frequencyTab);

        // Add tabbed pane to history panel
        historyPanel.add(tabbedPane, BorderLayout.CENTER);

        // Add history panel to the main panel
        add(historyPanel, BorderLayout.EAST);
    }

    private void clearHistory() {
        historyModel.clear();
        searchHistory.clear();
        frequencyModel.setRowCount(0);
    }

    private void addToHistory(String query) {
        query = query.toLowerCase().trim();
        SearchHistoryEntry entry = searchHistory.get(query);

        if (entry == null) {
            // New search term
            entry = new SearchHistoryEntry(query);
            searchHistory.put(query, entry);
        } else {
            // Existing search term - update frequency and time
            entry.incrementFrequency();
            entry.updateLastSearchTime();
        }

        updateHistoryDisplay();
    }

    private void updateHistoryDisplay() {
        // Update history list
        historyModel.clear();
        searchHistory.values().stream()
                .sorted(Comparator.comparing(SearchHistoryEntry::getLastSearchTime).reversed())
                .forEach(historyModel::addElement);

        // Update frequency table
        frequencyModel.setRowCount(0);
        searchHistory.values().stream()
                .sorted(Comparator.comparing(SearchHistoryEntry::getFrequency).reversed())
                .forEach(entry -> frequencyModel.addRow(new Object[]{
                        entry.getQuery(),
                        entry.getFrequency(),
                        entry.getLastSearchTime()
                }));
    }

    private static class SearchHistoryEntry {
        private final String query;
        private int frequency;
        private String lastSearchTime;

        public SearchHistoryEntry(String query) {
            this.query = query;
            this.frequency = 1;
            updateLastSearchTime();
        }

        public void incrementFrequency() {
            this.frequency++;
        }

        public void updateLastSearchTime() {
            this.lastSearchTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date());
        }

        public String getQuery() { return query; }
        public int getFrequency() { return frequency; }
        public String getLastSearchTime() { return lastSearchTime; }

        @Override
        public String toString() {
            return query;
        }
    }


    public SearchPanel(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        this.wordCompletion = new WordCompletion();
        this.products = new ArrayList<>();
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeUI();
        loadProductData();

        // Create main content panel with search results and popular products
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));

        // Add search results to center
        contentPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Add popular products to right
        contentPanel.add(createPopularProductsPanel(), BorderLayout.EAST);

        // Add the content panel
        add(contentPanel, BorderLayout.CENTER);

        addFilterPanel();
        addHistoryPanel();
    }

    private JPanel createPopularProductsPanel() {
        JPanel containerPanel = new JPanel(new BorderLayout());

        JPanel popularPanel = new JPanel();
        popularPanel.setLayout(new BoxLayout(popularPanel, BoxLayout.Y_AXIS));
        popularPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Popular Products",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)
        ));
        popularPanel.setBackground(Color.WHITE);

        // Get 5 random products
        List<Product> randomProducts = getRandomProducts(5);

        for (Product product : randomProducts) {
            JPanel productCard = createProductCard(product);
            popularPanel.add(productCard);
            popularPanel.add(Box.createVerticalStrut(10)); // Add spacing between cards
        }

        // Wrap in a scroll pane
        JScrollPane scrollPane = new JScrollPane(popularPanel);
        scrollPane.setPreferredSize(new Dimension(250, 400));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Add scroll pane to container panel
        containerPanel.add(scrollPane, BorderLayout.CENTER);

        return containerPanel;
    }

    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(new Color(248, 249, 250));
        card.setMaximumSize(new Dimension(230, 150));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Product name
        JLabel nameLabel = new JLabel(product.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Price
        JLabel priceLabel = new JLabel(String.format("$%.2f", product.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(40, 167, 69));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Category
        JLabel categoryLabel = new JLabel(product.getCategory());
        categoryLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        categoryLabel.setForeground(Color.GRAY);
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Features panel with flow layout
        JPanel featuresPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        featuresPanel.setBackground(new Color(248, 249, 250));
        featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add up to 3 features as small badges
        int featureCount = 0;
        for (String feature : product.getFeatures()) {
            if (featureCount >= 3) break;
            JLabel featureLabel = createFeatureBadge(feature);
            featuresPanel.add(featureLabel);
            featureCount++;
        }

        // Add components to card
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(priceLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(categoryLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(featuresPanel);

        // Add click listener
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                displayProductDetails(product);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(240, 240, 240));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(248, 249, 250));
            }
        });

        return card;
    }

    private JLabel createFeatureBadge(String feature) {
        JLabel badge = new JLabel(feature);
        badge.setFont(new Font("Arial", Font.PLAIN, 10));
        badge.setForeground(new Color(0, 123, 255));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 123, 255), 1),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        badge.setOpaque(true);
        badge.setBackground(new Color(240, 248, 255));
        return badge;
    }

    private List<Product> getRandomProducts(int count) {
        List<Product> randomProducts = new ArrayList<>();
        if (products.isEmpty()) return randomProducts;

        // Create a copy of the products list to avoid modifying the original
        List<Product> availableProducts = new ArrayList<>(products);
        Random random = new Random();

        // Get random products
        for (int i = 0; i < count && !availableProducts.isEmpty(); i++) {
            int randomIndex = random.nextInt(availableProducts.size());
            randomProducts.add(availableProducts.remove(randomIndex));
        }

        return randomProducts;
    }

    private void displayProductDetails(Product product) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Product Details");
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add product details
        JLabel nameLabel = new JLabel(product.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel priceLabel = new JLabel(String.format("Price: $%.2f", product.getPrice()));
        JLabel categoryLabel = new JLabel("Category: " + product.getCategory());

        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        detailsPanel.add(priceLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(categoryLabel);
        detailsPanel.add(Box.createVerticalStrut(10));

        // Add features
        JLabel featuresTitle = new JLabel("Features:");
        detailsPanel.add(featuresTitle);
        for (String feature : product.getFeatures()) {
            detailsPanel.add(new JLabel("• " + feature));
        }

        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.add(detailsPanel, BorderLayout.CENTER);
        dialog.add(closeButton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void addFilterPanel() {
        // Create filter panel
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        // Company filter
        JPanel companyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        companyPanel.add(new JLabel("Company:"));
        Set<String> companies = new HashSet<>();
        companies.add("All");
        products.forEach(p -> companies.add(p.getCategory()));
        JComboBox<String> companyCombo = new JComboBox<>(companies.toArray(new String[0]));
        companyPanel.add(companyCombo);

        // Price range filter
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pricePanel.add(new JLabel("Price Range:"));
        JTextField minPrice = new JTextField(8);
        JTextField maxPrice = new JTextField(8);
        pricePanel.add(new JLabel("Min $"));
        pricePanel.add(minPrice);
        pricePanel.add(new JLabel("Max $"));
        pricePanel.add(maxPrice);

        // Features filter
        JPanel featuresPanel = new JPanel(new BorderLayout());
        featuresPanel.setBorder(BorderFactory.createTitledBorder("Features"));

        // Collect all unique features
        Set<String> allFeatures = new HashSet<>();
        products.forEach(p -> allFeatures.addAll(p.getFeatures()));

        // Create checkboxes for features
        Map<String, JCheckBox> featureCheckboxes = new HashMap<>();
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        for (String feature : allFeatures) {
            JCheckBox checkbox = new JCheckBox(feature);
            featureCheckboxes.put(feature, checkbox);
            checkboxPanel.add(checkbox);
        }

        // Make features scrollable
        JScrollPane featuresScrollPane = new JScrollPane(checkboxPanel);
        featuresScrollPane.setPreferredSize(new Dimension(300, 200));
        featuresScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        featuresPanel.add(featuresScrollPane, BorderLayout.CENTER);

        // Add "Apply Filters" button
        JButton applyButton = new JButton("Apply Filters");
        applyButton.addActionListener(e -> {
            String selectedCompany = (String) companyCombo.getSelectedItem();

            // Get price range
            double min = -1, max = Double.MAX_VALUE;
            try {
                if (!minPrice.getText().isEmpty()) {
                    min = Double.parseDouble(minPrice.getText());
                }
                if (!maxPrice.getText().isEmpty()) {
                    max = Double.parseDouble(maxPrice.getText());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid price values",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get selected features
            Set<String> selectedFeatures = featureCheckboxes.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // Apply filters
            List<Product> filteredResults = filterProducts(
                    selectedCompany,
                    min,
                    max,
                    selectedFeatures
            );

            // Display filtered results
            displayResults(filteredResults);
        });

        // Reset filters button
        JButton resetButton = new JButton("Reset Filters");
        resetButton.addActionListener(e -> {
            companyCombo.setSelectedItem("All");
            minPrice.setText("");
            maxPrice.setText("");
            featureCheckboxes.values().forEach(cb -> cb.setSelected(false));
            displayResults(products);
        });

        // Create a main content panel for the filters
        JPanel filtersContentPanel = new JPanel();
        filtersContentPanel.setLayout(new BoxLayout(filtersContentPanel, BoxLayout.Y_AXIS));

        // Add filter components to the content panel
        filtersContentPanel.add(companyPanel);
        filtersContentPanel.add(Box.createVerticalStrut(10));
        filtersContentPanel.add(pricePanel);
        filtersContentPanel.add(Box.createVerticalStrut(10));
        filtersContentPanel.add(featuresPanel);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(applyButton);
        buttonPanel.add(resetButton);

        // Use BorderLayout for the main filter panel
        filterPanel.setLayout(new BorderLayout());
        filterPanel.add(filtersContentPanel, BorderLayout.CENTER);
        filterPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add filter panel to main panel
        add(filterPanel, BorderLayout.WEST);
    }

    private List<Product> filterProducts(String company, double minPrice, double maxPrice, Set<String> selectedFeatures) {
        return products.stream()
                .filter(product -> {
                    // Filter by company
                    if (!"All".equals(company) && !product.getCategory().equals(company)) {
                        return false;
                    }

                    // Filter by price range
                    if (minPrice >= 0 && product.getPrice() < minPrice) {
                        return false;
                    }
                    if (maxPrice < Double.MAX_VALUE && product.getPrice() > maxPrice) {
                        return false;
                    }

                    // Filter by features
                    if (!selectedFeatures.isEmpty()) {
                        return product.getFeatures().stream()
                                .anyMatch(selectedFeatures::contains);
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }




    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search panel with spell check and autocomplete
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));

        // Create search field panel
        JPanel searchFieldPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());

        // Add spell check label
        spellCheckLabel = new JLabel();
        spellCheckLabel.setForeground(Color.RED);
        spellCheckLabel.setVisible(false);

        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchFieldPanel.add(searchButton, BorderLayout.EAST);
        searchFieldPanel.add(spellCheckLabel, BorderLayout.SOUTH);

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

        // Initialize spell check timer
        spellCheckTimer = new Timer(SPELL_CHECK_DELAY, e -> performSpellCheck());
        spellCheckTimer.setRepeats(false);

        // Add document listener for search field
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSuggestionsAndSpellCheck();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSuggestionsAndSpellCheck();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSuggestionsAndSpellCheck();
            }
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

        // Add focus listener
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
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

        searchPanel.add(searchFieldPanel, BorderLayout.NORTH);

        // Results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        JScrollPane resultScroll = new JScrollPane(resultArea);

        add(searchPanel, BorderLayout.NORTH);
        add(resultScroll, BorderLayout.CENTER);
    }

    private void updateSuggestionsAndSpellCheck() {
        // Reset spell check timer
        spellCheckTimer.restart();

        // Update suggestions
        updateSuggestions();
    }

    private void performSpellCheck() {
        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            // Split the search query into words
            String[] words = text.toLowerCase().split("\\s+");
            List<String> misspelledWords = new ArrayList<>();
            Map<String, List<String>> suggestions = new HashMap<>();

            // Check each word
            for (String word : words) {
                if (!searchEngine.getSpellChecker().isWordValid(word)) {
                    misspelledWords.add(word);
                    suggestions.put(word, searchEngine.getSpellChecker().getSuggestions(word));
                }
            }

            // If there are misspelled words, show suggestions
            if (!misspelledWords.isEmpty()) {
                StringBuilder suggestionText = new StringBuilder("<html>Did you mean: ");
                for (int i = 0; i < misspelledWords.size(); i++) {
                    String word = misspelledWords.get(i);
                    List<String> wordSuggestions = suggestions.get(word);
                    if (!wordSuggestions.isEmpty()) {
                        if (i > 0) suggestionText.append(", ");
                        suggestionText.append("<b>").append(wordSuggestions.get(0)).append("</b>");
                    }
                }
                suggestionText.append("?</html>");
                spellCheckLabel.setText(suggestionText.toString());
                spellCheckLabel.setVisible(true);
                return;
            }
        }
        spellCheckLabel.setVisible(false);
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
            String jsonContent = new String(Files.readAllBytes(Paths.get("ACC-Project\\audio_products.json")));

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

            // Initialize spell checker with product data
            initializeSpellChecker(products);

            System.out.println("Loaded " + products.size() + " products");

        } catch (Exception e) {
            System.err.println("Error loading product data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSpellChecker(List<Product> products) {
        // Build vocabulary from product data
        Set<String> vocabulary = new HashSet<>();

        for (Product product : products) {
            // Add words from product name
            addWordsToVocabulary(product.getName(), vocabulary);

            // Add words from features
            for (String feature : product.getFeatures()) {
                addWordsToVocabulary(feature, vocabulary);
            }

            // Add words from specifications
            for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                addWordsToVocabulary(spec.getValue(), vocabulary);
            }

            // Add category
            addWordsToVocabulary(product.getCategory(), vocabulary);
        }

        // Add common audio/technology terms
        String[] commonTerms = {
                "wireless", "bluetooth", "speaker", "soundbar", "subwoofer",
                "surround", "dolby", "atmos", "bass", "treble", "audio",
                "digital", "optical", "hdmi", "remote", "control", "power",
                "watts", "channels", "configuration", "system"
        };
        Collections.addAll(vocabulary, commonTerms);

        // Build the spell checker vocabulary through SearchEngine
        searchEngine.getSpellChecker().buildVocabulary(products);
        System.out.println("Spell checker initialized with vocabulary size: " + vocabulary.size());
    }

    private void addWordsToVocabulary(String text, Set<String> vocabulary) {
        if (text == null) return;

        // Split text into words and clean
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // Skip very short words
                vocabulary.add(word);
            }
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

        addToHistory(query);
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
                ImageIcon productImage = new ImageIcon("src/stock.jpg"); // Path to the default image
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