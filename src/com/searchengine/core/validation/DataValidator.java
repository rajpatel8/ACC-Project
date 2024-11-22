package com.searchengine.core.validation;

import com.searchengine.model.Product;
import java.util.*;
import java.util.regex.Pattern;

public class DataValidator {
    private final Map<String, Pattern> patterns;
    private final ValidationRules rules;

    public DataValidator() {
        this.patterns = ValidationPatterns.getDefaultPatterns();
        this.rules = new ValidationRules();
    }

    public ValidationResult validateProduct(Product product) {
        ValidationResult result = new ValidationResult();

        // Required fields validation
        if (product.getName() == null || product.getName().isEmpty()) {
            result.addError("Name is required");
        } else if (!patterns.get("name").matcher(product.getName()).matches()) {
            result.addError("Invalid product name format");
        }

        if (product.getPrice() <= 0) {
            result.addError("Price must be greater than 0");
        }

        if (product.getProductId() == null || !patterns.get("productId")
                .matcher(product.getProductId()).matches()) {
            result.addError("Invalid product ID format");
        }

        // URL validation
        if (product.getUrl() != null && !patterns.get("url")
                .matcher(product.getUrl()).matches()) {
            result.addError("Invalid URL format");
        }

        // Features validation
        for (String feature : product.getFeatures()) {
            if (!patterns.get("feature").matcher(feature).matches()) {
                result.addWarning("Feature format may be invalid: " + feature);
            }
        }

        return result;
    }

    public boolean isValidProductId(String productId) {
        return productId != null &&
                patterns.get("productId").matcher(productId).matches();
    }

    public boolean isValidPrice(String price) {
        return price != null &&
                patterns.get("price").matcher(price).matches();
    }

    public boolean isValidUrl(String url) {
        return url != null &&
                patterns.get("url").matcher(url).matches();
    }
}