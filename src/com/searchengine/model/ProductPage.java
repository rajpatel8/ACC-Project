package com.searchengine.model;

import java.util.*;

public class ProductPage extends WebPage {
    private List<Product> products;
    private String pageType; // PRODUCT_LIST, PRODUCT_DETAIL, CATEGORY
    private Map<String, String> filters;
    private String categoryPath;
    private int totalProducts;
    private int currentPage;
    private int totalPages;
    private String sortBy;

    public ProductPage() {
        super();
        this.products = new ArrayList<>();
        this.filters = new HashMap<>();
    }

    // Getters and Setters
    public List<Product> getProducts() { return products; }
    public void addProduct(Product product) { this.products.add(product); }

    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }

    public Map<String, String> getFilters() { return filters; }
    public void addFilter(String key, String value) { this.filters.put(key, value); }

    public String getCategoryPath() { return categoryPath; }
    public void setCategoryPath(String categoryPath) { this.categoryPath = categoryPath; }

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}