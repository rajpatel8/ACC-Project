package com.searchengine.core.completion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.searchengine.model.Product;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProductWordCompletion {
    private final WordCompletion wordCompletion;
    private List<Product> products;

    public ProductWordCompletion() {
        this.wordCompletion = new WordCompletion();
        this.products = new ArrayList<>();
    }

    public void initializeFromJson(String jsonContent) {
        try {
            // Parse JSON
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            JsonObject companies = root.getAsJsonObject("company");

            // Convert JSON to Product objects
            for (Map.Entry<String, JsonElement> company : companies.entrySet()) {
                String companyName = company.getKey();
                JsonObject companyData = company.getValue().getAsJsonObject();

                for (JsonElement productElement : companyData.getAsJsonArray("products")) {
                    Product product = new Product();
                    JsonObject productJson = productElement.getAsJsonObject();

                    // Set basic product properties
                    product.setName(productJson.get("name").getAsString());
                    product.setPrice(productJson.get("price").getAsDouble());
                    product.setCategory(companyName);

                    // Add features from audio technology
                    if (productJson.has("audioTechnology")) {
                        for (JsonElement tech : productJson.getAsJsonArray("audioTechnology")) {
                            product.addFeature(tech.getAsString());
                        }
                    }

                    // Add connectivity as features
                    if (productJson.has("connectivity")) {
                        for (JsonElement conn : productJson.getAsJsonArray("connectivity")) {
                            product.addFeature("Connectivity: " + conn.getAsString());
                        }
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

            // Build the trie with products
            wordCompletion.buildTrie(products);

            System.out.println("Successfully initialized word completion with " +
                    products.size() + " products");

        } catch (Exception e) {
            System.err.println("Error initializing word completion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> getSuggestions(String prefix) {
        return wordCompletion.getSuggestions(prefix).stream()
                .map(suggestion -> suggestion.getWord())
                .toList();
    }

    public static void main(String[] args) {
        try {
            // Test the implementation
            String jsonContent = Files.readString(Paths.get("audio_products.json"));
            ProductWordCompletion completion = new ProductWordCompletion();
            completion.initializeFromJson(jsonContent);

            // Test some prefixes
            String[] testPrefixes = {"sou", "blu", "dol", "wirele"};
            for (String prefix : testPrefixes) {
                System.out.println("\nSuggestions for '" + prefix + "':");
                List<String> suggestions = completion.getSuggestions(prefix);
                suggestions.forEach(suggestion -> System.out.println("- " + suggestion));
            }

        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}