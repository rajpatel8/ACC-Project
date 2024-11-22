package com.searchengine.model;

import java.util.*;

public class Product {
    private String productId;
    private String name;
    private String url;
    private double price;
    private String description;
    private List<String> features;
    private Map<String, String> specifications;
    private List<String> images;
    private String category;
    private boolean inStock;
    private String currency;
    private double rating;
    private int reviewCount;
    private Map<String, String> variants;
    private long lastUpdated;

    public Product() {
        this.features = new ArrayList<>();
        this.specifications = new HashMap<>();
        this.images = new ArrayList<>();
        this.variants = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getFeatures() { return features; }
    public void addFeature(String feature) { this.features.add(feature); }

    public Map<String, String> getSpecifications() { return specifications; }
    public void addSpecification(String key, String value) {
        this.specifications.put(key, value);
    }

    public List<String> getImages() { return images; }
    public void addImage(String image) { this.images.add(image); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public Map<String, String> getVariants() { return variants; }
    public void addVariant(String type, String value) {
        this.variants.put(type, value);
    }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Product Details:\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Price: ").append(currency).append(" ").append(price).append("\n");
        sb.append("Category: ").append(category).append("\n");
        if (!features.isEmpty()) {
            sb.append("Features:\n");
            features.forEach(f -> sb.append("- ").append(f).append("\n"));
        }
        if (!specifications.isEmpty()) {
            sb.append("Specifications:\n");
            specifications.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }
}
