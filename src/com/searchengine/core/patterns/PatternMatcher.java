package com.searchengine.core.patterns;

import com.searchengine.model.Product;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class PatternMatcher {
    // Common validation patterns
    private static final Map<String, Pattern> VALIDATION_PATTERNS = new HashMap<>() {{
        put("model", Pattern.compile("^[A-Z0-9]+-[A-Z0-9]+$"));
        put("price", Pattern.compile("^\\$?\\d+(?:\\.\\d{2})?$"));
        put("url", Pattern.compile("^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*$"));
    }};

    // Product-specific patterns
    private static final Map<String, Pattern> FEATURE_PATTERNS = new HashMap<>() {{
        put("bluetooth", Pattern.compile("(?i)bluetooth|wireless"));
        put("dolby", Pattern.compile("(?i)dolby|atmos|surround"));
        put("power", Pattern.compile("(?i)(\\d+)\\s*W(atts)?"));
        put("channels", Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*ch(annel)?s?"));
    }};

    public List<Product> findProductsMatchingPattern(List<Product> products, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return products.stream()
                .filter(product -> matchesPattern(product, compiledPattern))
                .collect(Collectors.toList());
    }

    private boolean matchesPattern(Product product, Pattern pattern) {
        // Check name
        if (pattern.matcher(product.getName()).find()) return true;

        // Check description
        if (product.getDescription() != null &&
                pattern.matcher(product.getDescription()).find()) return true;

        // Check features
        for (String feature : product.getFeatures()) {
            if (pattern.matcher(feature).find()) return true;
        }

        // Check specifications
        for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
            if (pattern.matcher(spec.getKey()).find() ||
                    pattern.matcher(spec.getValue()).find()) return true;
        }

        return false;
    }

    public Map<String, List<Product>> categorizeByFeatures(List<Product> products) {
        Map<String, List<Product>> categorized = new HashMap<>();

        for (Map.Entry<String, Pattern> feature : FEATURE_PATTERNS.entrySet()) {
            List<Product> matching = products.stream()
                    .filter(product -> hasFeature(product, feature.getValue()))
                    .collect(Collectors.toList());
            if (!matching.isEmpty()) {
                categorized.put(feature.getKey(), matching);
            }
        }

        return categorized;
    }

    private boolean hasFeature(Product product, Pattern pattern) {
        // Check features
        for (String feature : product.getFeatures()) {
            if (pattern.matcher(feature).find()) return true;
        }

        // Check description
        if (product.getDescription() != null &&
                pattern.matcher(product.getDescription()).find()) return true;

        return false;
    }

    public Map<String, String> extractProductMetrics(Product product) {
        Map<String, String> metrics = new HashMap<>();

        // Extract power rating
        Pattern powerPattern = FEATURE_PATTERNS.get("power");
        Pattern channelsPattern = FEATURE_PATTERNS.get("channels");

        // Check features and description
        List<String> textsToCheck = new ArrayList<>(product.getFeatures());
        if (product.getDescription() != null) {
            textsToCheck.add(product.getDescription());
        }

        for (String text : textsToCheck) {
            // Extract power
            Matcher powerMatcher = powerPattern.matcher(text);
            if (powerMatcher.find() && !metrics.containsKey("power")) {
                metrics.put("power", powerMatcher.group(1) + "W");
            }

            // Extract channels
            Matcher channelsMatcher = channelsPattern.matcher(text);
            if (channelsMatcher.find() && !metrics.containsKey("channels")) {
                metrics.put("channels", channelsMatcher.group(1) + " channels");
            }
        }

        return metrics;
    }

    public boolean validateProduct(Product product) {
        if (product.getName() == null || product.getName().isEmpty()) {
            return false;
        }

        if (product.getPrice() <= 0) {
            return false;
        }

        // Validate URL if present
        if (product.getUrl() != null && !VALIDATION_PATTERNS.get("url")
                .matcher(product.getUrl()).matches()) {
            return false;
        }

        // Validate price format
        String priceStr = String.format("$%.2f", product.getPrice());
        if (!VALIDATION_PATTERNS.get("price").matcher(priceStr).matches()) {
            return false;
        }

        return true;
    }

    public List<PatternMatch> findPatterns(Product product) {
        List<PatternMatch> matches = new ArrayList<>();

        // Check each feature pattern
        for (Map.Entry<String, Pattern> feature : FEATURE_PATTERNS.entrySet()) {
            Pattern pattern = feature.getValue();
            String featureType = feature.getKey();

            // Check product name
            addMatches(matches, pattern, product.getName(), "name", featureType);

            // Check description
            if (product.getDescription() != null) {
                addMatches(matches, pattern, product.getDescription(),
                        "description", featureType);
            }

            // Check features
            for (String feat : product.getFeatures()) {
                addMatches(matches, pattern, feat, "feature", featureType);
            }
        }

        return matches;
    }

    private void addMatches(List<PatternMatch> matches, Pattern pattern,
                            String text, String field, String featureType) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(new PatternMatch(
                    featureType,
                    field,
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }
    }
}
