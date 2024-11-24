package com.searchengine.core;

import com.searchengine.model.Product;

import java.io.IOException;
import java.util.*;
import java.nio.file.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ProductDataManager {
    private final String cacheDir = "html_pages";
    private final String productDir = "product_data";
    private final Map<String, List<Product>> companyProducts;
    private final Gson gson;

    public ProductDataManager() {
        this.companyProducts = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(productDir));
            Files.createDirectories(Paths.get(productDir, "structured"));
            Files.createDirectories(Paths.get(productDir, "index"));
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
    }

    public void processAllProducts() {
        try {
            // Process each company's cached pages
            Files.list(Paths.get(cacheDir))
                    .filter(Files::isDirectory)
                    .forEach(this::processCompanyDirectory);

            // Save structured data
            saveStructuredData();

            // Generate product index
            generateProductIndex();

        } catch (IOException e) {
            System.err.println("Error processing products: " + e.getMessage());
        }
    }

    private void processCompanyDirectory(Path companyDir) {
        String company = companyDir.getFileName().toString().toUpperCase();
        List<Product> products = new ArrayList<>();

        try {
            // Process main page and product pages
            Files.walk(companyDir)
                    .filter(p -> p.toString().endsWith(".html"));

            companyProducts.put(company, products);
            System.out.println("Processed " + products.size() + " products for " + company);

        } catch (IOException e) {
            System.err.println("Error processing company: " + company);
        }
    }

    private void saveStructuredData() {
        try {
            for (Map.Entry<String, List<Product>> entry : companyProducts.entrySet()) {
                String company = entry.getKey();
                List<Product> products = entry.getValue();

                // Save as JSON
                Path jsonPath = Paths.get(productDir, "structured",
                        company.toLowerCase() + "_products.json");
                String json = gson.toJson(products);
                Files.writeString(jsonPath, json);

                // Save as CSV for easy viewing
                Path csvPath = Paths.get(productDir, "structured",
                        company.toLowerCase() + "_products.csv");
                saveProductsAsCsv(products, csvPath);
            }
        } catch (IOException e) {
            System.err.println("Error saving structured data: " + e.getMessage());
        }
    }

    private void saveProductsAsCsv(List<Product> products, Path path)
            throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Price,Category,Features\n");

        for (Product product : products) {
            csv.append(String.format("%s,%s,%.2f,%s,\"%s\"\n",
                    escapeCSV(product.getProductId()),
                    escapeCSV(product.getName()),
                    product.getPrice(),
                    escapeCSV(product.getCategory()),
                    escapeCSV(String.join("; ", product.getFeatures()))
            ));
        }

        Files.writeString(path, csv.toString());
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void generateProductIndex() {
        try {
            // Create word-to-product index
            Map<String, Set<String>> wordIndex = new HashMap<>();

            for (List<Product> products : companyProducts.values()) {
                for (Product product : products) {
                    indexProduct(product, wordIndex);
                }
            }

            // Save index
            Path indexPath = Paths.get(productDir, "index", "word_index.json");
            String json = gson.toJson(wordIndex);
            Files.writeString(indexPath, json);

        } catch (IOException e) {
            System.err.println("Error generating index: " + e.getMessage());
        }
    }

    private void indexProduct(Product product, Map<String, Set<String>> wordIndex) {
        // Index product name words
        indexText(product.getName(), product.getProductId(), wordIndex);

        // Index features
        for (String feature : product.getFeatures()) {
            indexText(feature, product.getProductId(), wordIndex);
        }

        // Index description
        indexText(product.getDescription(), product.getProductId(), wordIndex);
    }

    private void indexText(String text, String productId,
                           Map<String, Set<String>> wordIndex) {
        if (text == null) return;

        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2) {  // Skip very short words
                wordIndex.computeIfAbsent(word, k -> new HashSet<>())
                        .add(productId);
            }
        }
    }

    // Getters for other components to use
    public Map<String, List<Product>> getCompanyProducts() {
        return companyProducts;
    }

    public List<Product> getAllProducts() {
        return companyProducts.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}