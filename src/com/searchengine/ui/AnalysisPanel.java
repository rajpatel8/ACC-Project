package com.searchengine.ui;

import com.searchengine.core.search.SearchEngine;
import com.searchengine.core.frequency.*;
import com.searchengine.core.patterns.*;
import com.searchengine.model.Product;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class AnalysisPanel extends JPanel {
    private final SearchEngine searchEngine;
    private final FrequencyAnalyzer frequencyAnalyzer;
    private final PatternMatcher patternMatcher;
    private JTabbedPane tabbedPane;
    private JTable frequencyTable;
    private JTable patternTable;
    private JTextArea statsArea;
    private DefaultTableModel frequencyModel;
    private DefaultTableModel patternModel;
    private Map<String, JTable> attributeTables;
    private Map<String, DefaultTableModel> attributeTableModels;

    public AnalysisPanel(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
        this.frequencyAnalyzer = new FrequencyAnalyzer();
        this.patternMatcher = new PatternMatcher();
        this.attributeTables = new HashMap<>();
        this.attributeTableModels = new HashMap<>();
        initializeUI();
        loadAndAnalyzeData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create main tabbed pane
        tabbedPane = new JTabbedPane();

        // Add word frequency tab with all frequency analysis
        tabbedPane.addTab("Word Frequencies", createFrequencyPanel());

        // Add patterns tab
        tabbedPane.addTab("Patterns", createPatternPanel());

        // Add statistics tab
        tabbedPane.addTab("Statistics", createStatsPanel());

        // Add control panel
        add(createControlPanel(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createFrequencyPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create tabbed pane for frequency analyses with tabs at the top (center)
        JTabbedPane frequencyTabs = new JTabbedPane(JTabbedPane.TOP);

        // Add word frequency tab
        JPanel wordFreqPanel = new JPanel(new BorderLayout());
        String[] columns = {"Word", "Frequency", "Products"};
        frequencyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        frequencyTable = new JTable(frequencyModel);
        frequencyTable.setAutoCreateRowSorter(true);

        // Add sorting controls for word frequency
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> sortBox = new JComboBox<>(new String[]{
                "Sort by Frequency",
                "Sort Alphabetically",
                "Sort by Product Count"
        });
        sortBox.addActionListener(e -> sortFrequencyTable(sortBox.getSelectedIndex()));
        controlPanel.add(sortBox);

        wordFreqPanel.add(controlPanel, BorderLayout.NORTH);
        wordFreqPanel.add(new JScrollPane(frequencyTable), BorderLayout.CENTER);
        frequencyTabs.addTab("Word Frequency", wordFreqPanel);

        // Add attribute analysis tabs
        createAttributeTable(frequencyTabs, "Speaker Configuration", "Configuration", "Count", "Percentage");
        createAttributeTable(frequencyTabs, "Power Output", "Power Range", "Count", "Percentage");
        createAttributeTable(frequencyTabs, "Audio Technology", "Technology", "Count", "Percentage");
        createAttributeTable(frequencyTabs, "Connectivity", "Connection Type", "Count", "Percentage");
        createAttributeTable(frequencyTabs, "Product Type", "Type", "Count", "Percentage");

        mainPanel.add(frequencyTabs, BorderLayout.CENTER);
        return mainPanel;
    }

    private void sortFrequencyTable(int sortType) {
        ArrayList<Object[]> data = new ArrayList<>();
        for (int i = 0; i < frequencyModel.getRowCount(); i++) {
            Object[] row = new Object[frequencyModel.getColumnCount()];
            for (int j = 0; j < row.length; j++) {
                row[j] = frequencyModel.getValueAt(i, j);
            }
            data.add(row);
        }

        switch (sortType) {
            case 0: // Frequency
                data.sort((r1, r2) -> Integer.compare(
                        (Integer) r2[1], (Integer) r1[1]));
                break;
            case 1: // Alphabetical
                data.sort((r1, r2) -> ((String) r1[0])
                        .compareToIgnoreCase((String) r2[0]));
                break;
            case 2: // Product count
                data.sort((r1, r2) -> ((String) r2[2]).split(",").length -
                        ((String) r1[2]).split(",").length);
                break;
        }

        frequencyModel.setRowCount(0);
        for (Object[] row : data) {
            frequencyModel.addRow(row);
        }
    }

    private JPanel createPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create pattern table
        String[] columns = {"Pattern Type", "Found Text", "Location", "Count"};
        patternModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        patternTable = new JTable(patternModel);
        patternTable.setAutoCreateRowSorter(true);

        panel.add(new JScrollPane(patternTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(statsArea), BorderLayout.CENTER);
        return panel;
    }

    private void createAttributeTable(JTabbedPane parentTab, String tabName, String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        // Create scroll pane with some padding
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a panel for the table with padding
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scrollPane, BorderLayout.CENTER);

        parentTab.addTab(tabName, panel);

        attributeTables.put(tabName, table);
        attributeTableModels.put(tabName, model);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(e -> performAnalysis());

        JButton exportButton = new JButton("Export Results");
        exportButton.addActionListener(e -> exportResults());

        panel.add(analyzeButton);
        panel.add(exportButton);

        return panel;
    }

    private void performAnalysis() {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get("ACC-Project/audio_products.json")));

            // Parse and analyze data
            List<Product> products = parseJsonToProducts(jsonContent);
            frequencyAnalyzer.analyzeProducts(products);

            // Update all views
            updateFrequencyTable();
            updatePatternTable(products);
            updateStats(products);
            analyzeAttributes(jsonContent);

            JOptionPane.showMessageDialog(this,
                    "Analysis completed successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error performing analysis: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void loadAndAnalyzeData() {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get("ACC-Project/audio_products.json")));

            // Parse JSON and create Product objects
            List<Product> products = parseJsonToProducts(jsonContent);

            // Analyze frequencies
            frequencyAnalyzer.analyzeProducts(products);
            updateFrequencyTable();

            // Analyze patterns
            updatePatternTable(products);

            // Update statistics
            updateStats(products);

            // Analyze attributes
            analyzeAttributes(jsonContent);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading or analyzing data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Product> parseJsonToProducts(String jsonContent) {
        List<Product> products = new ArrayList<>();
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
        JsonObject companies = root.getAsJsonObject("company");

        for (Map.Entry<String, JsonElement> company : companies.entrySet()) {
            String companyName = company.getKey();
            JsonObject companyData = company.getValue().getAsJsonObject();

            for (JsonElement productElement : companyData.getAsJsonArray("products")) {
                JsonObject productJson = productElement.getAsJsonObject();
                Product product = createProductFromJson(productJson, companyName);
                products.add(product);
            }
        }
        return products;
    }

    private Product createProductFromJson(JsonObject productJson, String company) {
        Product product = new Product();
        product.setName(productJson.get("name").getAsString());
        product.setPrice(productJson.get("price").getAsDouble());
        product.setCategory(company);

        // Add audio technologies as features
        for (JsonElement tech : productJson.getAsJsonArray("audioTechnology")) {
            product.addFeature(tech.getAsString());
        }

        // Add connectivity as features
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

        return product;
    }

    private void updateFrequencyTable() {
        frequencyModel.setRowCount(0);
        for (FrequencyResult result : frequencyAnalyzer.getTopWords(100)) {
            frequencyModel.addRow(new Object[]{
                    result.getWord(),
                    result.getFrequency(),
                    getProductsContainingWord(result.getWord())
            });
        }
    }

    private String getProductsContainingWord(String word) {
        List<String> productNames = new ArrayList<>();
        word = word.toLowerCase();

        for (Product product : searchEngine.getProducts()) {
            String finalWord = word;
            String finalWord1 = word;
            if (product.getName().toLowerCase().contains(word) ||
                    product.getFeatures().stream()
                            .anyMatch(f -> f.toLowerCase().contains(finalWord)) ||
                    product.getSpecifications().values().stream()
                            .anyMatch(s -> s.toLowerCase().contains(finalWord1))) {
                productNames.add(product.getName());
            }
        }

        return String.join(", ", productNames);
    }

    private void updatePatternTable(List<Product> products) {
        patternModel.setRowCount(0);
        Map<String, Integer> patternCounts = new HashMap<>();

        for (Product product : products) {
            List<PatternMatch> matches = patternMatcher.findPatterns(product);
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

    private void updateStats(List<Product> products) {
        StringBuilder stats = new StringBuilder();
        stats.append("Analysis Statistics\n");
        stats.append("==================\n\n");

        // Product statistics
        stats.append(String.format("Total Products: %d\n", products.size()));

        // Company statistics
        Map<String, Long> companyProducts = products.stream()
                .collect(groupingBy(Product::getCategory, counting()));

        stats.append("\nProducts by Company:\n");
        companyProducts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> stats.append(String.format("- %s: %d products\n",
                        entry.getKey(), entry.getValue())));

        // Price statistics
        DoubleSummaryStatistics priceStats = products.stream()
                .mapToDouble(Product::getPrice)
                .summaryStatistics();

        stats.append("\nPrice Statistics:\n");
        stats.append(String.format("- Average Price: $%.2f\n", priceStats.getAverage()));
        stats.append(String.format("- Minimum Price: $%.2f\n", priceStats.getMin()));
        stats.append(String.format("- Maximum Price: $%.2f\n", priceStats.getMax()));

        statsArea.setText(stats.toString());
    }

    private void analyzeAttributes(String jsonContent) {
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            JsonObject companies = root.getAsJsonObject("company");

            // Initialize counters
            Map<String, Integer> speakerConfigs = new HashMap<>();
            Map<String, Integer> powerOutputs = new TreeMap<>();
            Map<String, Integer> audioTechs = new HashMap<>();
            Map<String, Integer> connectivityTypes = new HashMap<>();
            Map<String, Integer> productTypes = new HashMap<>();
            int totalProducts = 0;

            // Process all products
            for (Map.Entry<String, JsonElement> company : companies.entrySet()) {
                JsonArray products = company.getValue().getAsJsonObject().getAsJsonArray("products");
                totalProducts += products.size();

                for (JsonElement productElement : products) {
                    JsonObject product = productElement.getAsJsonObject();

                    // Count speaker configurations
                    String config = product.get("speakerConfiguration").getAsString();
                    speakerConfigs.merge(config, 1, Integer::sum);

                    // Count power outputs (grouped in ranges)
                    String power = product.get("powerOutput").getAsString();
                    String powerRange = getPowerRange(power);
                    powerOutputs.merge(powerRange, 1, Integer::sum);

                    // Count audio technologies
                    JsonArray techArray = product.getAsJsonArray("audioTechnology");
                    for (JsonElement tech : techArray) {
                        audioTechs.merge(tech.getAsString(), 1, Integer::sum);
                    }

                    // Count connectivity types
                    JsonArray connArray = product.getAsJsonArray("connectivity");
                    for (JsonElement conn : connArray) {
                        connectivityTypes.merge(conn.getAsString(), 1, Integer::sum);
                    }

                    // Count product types
                    String type = product.get("type").getAsString();
                    productTypes.merge(type, 1, Integer::sum);
                }
            }

            // Update all attribute tables
            updateAttributeTable("Speaker Configuration", speakerConfigs, totalProducts);
            updateAttributeTable("Power Output", powerOutputs, totalProducts);
            updateAttributeTable("Audio Technology", audioTechs, totalProducts);
            updateAttributeTable("Connectivity", connectivityTypes, totalProducts);
            updateAttributeTable("Product Type", productTypes, totalProducts);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error analyzing attributes: " + e.getMessage());
        }
    }

    private void updateAttributeTable(String tableName, Map<String, Integer> data, int total) {
        DefaultTableModel model = attributeTableModels.get(tableName);
        model.setRowCount(0);

        // Sort by count in descending order
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(data.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            double percentage = (entry.getValue() * 100.0) / total;
            model.addRow(new Object[]{
                    entry.getKey(),
                    entry.getValue(),
                    String.format("%.1f%%", percentage)
            });
        }

        // Add total row
        model.addRow(new Object[]{"TOTAL", total, "100.0%"});
    }

    private String getPowerRange(String powerStr) {
        try {
            int power = Integer.parseInt(powerStr.replaceAll("[^0-9]", ""));
            if (power <= 50) return "0-50W";
            else if (power <= 100) return "51-100W";
            else if (power <= 250) return "101-250W";
            else if (power <= 500) return "251-500W";
            else if (power <= 1000) return "501-1000W";
            else return "1000W+";
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }

    // Helper methods (sortFrequencyTable, getProductsContainingWord, etc.)
    // remain the same as in previous implementations

    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                StringBuilder export = new StringBuilder();

                // Export frequency analysis
                export.append("Word Frequencies:\n");
                export.append("Word,Frequency,Products\n");
                for (int i = 0; i < frequencyModel.getRowCount(); i++) {
                    export.append(String.format("%s,%d,\"%s\"\n",
                            frequencyModel.getValueAt(i, 0),
                            frequencyModel.getValueAt(i, 1),
                            frequencyModel.getValueAt(i, 2)));
                }

                // Export pattern analysis
                export.append("\nPatterns Found:\n");
                export.append("Pattern Type,Found Text,Location,Count\n");
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

                // Export attribute analysis
                for (String tableName : attributeTableModels.keySet()) {
                    export.append("\n").append(tableName).append(":\n");
                    DefaultTableModel model = attributeTableModels.get(tableName);
                    export.append("Value,Count,Percentage\n");
                    for (int i = 0; i < model.getRowCount(); i++) {
                        export.append(String.format("%s,%s,%s\n",
                                model.getValueAt(i, 0),
                                model.getValueAt(i, 1),
                                model.getValueAt(i, 2)));
                    }
                }

                Files.write(fileChooser.getSelectedFile().toPath(),
                        export.toString().getBytes());

                JOptionPane.showMessageDialog(this,
                        "Analysis results exported successfully",
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting results: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}