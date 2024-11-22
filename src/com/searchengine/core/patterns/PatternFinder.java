package com.searchengine.core.patterns;

import com.searchengine.model.Product;
import java.util.*;
import java.util.regex.*;

public class PatternFinder {
    private final Map<String, Pattern> patterns;

    public PatternFinder() {
        patterns = new HashMap<>();
        initializePatterns();
    }

    private void initializePatterns() {
        // Audio patterns
        patterns.put("watts", Pattern.compile("(\\d+)\\s*(?:W|watts)", Pattern.CASE_INSENSITIVE));
        patterns.put("channels", Pattern.compile("(\\d+(?:\\.\\d+)?)(?:-channel|ch)", Pattern.CASE_INSENSITIVE));
        patterns.put("bluetooth", Pattern.compile("bluetooth\\s*(?:\\d+(?:\\.\\d+)?)?", Pattern.CASE_INSENSITIVE));
        patterns.put("dolby", Pattern.compile("dolby\\s+(?:atmos|digital|surround)", Pattern.CASE_INSENSITIVE));
        patterns.put("frequency", Pattern.compile("(\\d+(?:-\\d+)?)\\s*Hz", Pattern.CASE_INSENSITIVE));
        patterns.put("dimensions", Pattern.compile("(\\d+(?:\\.\\d+)?\\s*[xX]\\s*\\d+(?:\\.\\d+)?\\s*[xX]\\s*\\d+(?:\\.\\d+)?)\\s*(?:mm|cm|in)?"));
    }

    public List<PatternMatch> findPatterns(Product product) {
        List<PatternMatch> matches = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            String patternName = entry.getKey();
            Pattern pattern = entry.getValue();

            // Search in name
            findMatchesInField(product.getName(), "name", patternName, pattern, matches);

            // Search in description
            if (product.getDescription() != null) {
                findMatchesInField(product.getDescription(), "description", patternName, pattern, matches);
            }

            // Search in features
            for (String feature : product.getFeatures()) {
                findMatchesInField(feature, "feature", patternName, pattern, matches);
            }

            // Search in specifications
            for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                findMatchesInField(spec.getValue(), "specification", patternName, pattern, matches);
            }
        }

        return matches;
    }

    private void findMatchesInField(String text, String fieldName, String patternName,
                                    Pattern pattern, List<PatternMatch> matches) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(new PatternMatch(
                    patternName,
                    fieldName,
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }
    }

    public Map<String, String> extractMetrics(Product product) {
        Map<String, String> metrics = new HashMap<>();
        List<PatternMatch> matches = findPatterns(product);

        for (PatternMatch match : matches) {
            switch (match.getFeatureType()) {
                case "watts":
                    if (!metrics.containsKey("power")) {
                        metrics.put("power", match.getMatchedText());
                    }
                    break;
                case "channels":
                    if (!metrics.containsKey("channels")) {
                        metrics.put("channels", match.getMatchedText());
                    }
                    break;
                case "frequency":
                    if (!metrics.containsKey("frequency")) {
                        metrics.put("frequency", match.getMatchedText());
                    }
                    break;
                case "dimensions":
                    if (!metrics.containsKey("dimensions")) {
                        metrics.put("dimensions", match.getMatchedText());
                    }
                    break;
            }
        }

        return metrics;
    }

    public List<String> findFeatures(Product product) {
        Set<String> features = new HashSet<>();
        List<PatternMatch> matches = findPatterns(product);

        for (PatternMatch match : matches) {
            if (match.getFeatureType().equals("bluetooth") ||
                    match.getFeatureType().equals("dolby")) {
                features.add(match.getMatchedText());
            }
        }

        return new ArrayList<>(features);
    }

    public Map<String, List<Product>> categorizeProducts(List<Product> products, String patternType) {
        Map<String, List<Product>> categorized = new HashMap<>();
        Pattern pattern = patterns.get(patternType);

        if (pattern == null) {
            throw new IllegalArgumentException("Invalid pattern type: " + patternType);
        }

        for (Product product : products) {
            List<PatternMatch> matches = findPatterns(product);
            for (PatternMatch match : matches) {
                if (match.getFeatureType().equals(patternType)) {
                    categorized.computeIfAbsent(match.getMatchedText(), k -> new ArrayList<>())
                            .add(product);
                }
            }
        }

        return categorized;
    }
}
