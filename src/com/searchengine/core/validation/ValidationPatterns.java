package com.searchengine.core.validation;

import java.util.*;
import java.util.regex.Pattern;

public class ValidationPatterns {
    public static Map<String, Pattern> getDefaultPatterns() {
        Map<String, Pattern> patterns = new HashMap<>();

        patterns.put("productId", Pattern.compile("^[A-Z0-9]+-[A-Z0-9]+$"));
        patterns.put("name", Pattern.compile("^[\\w\\s-]{3,100}$"));
        patterns.put("price", Pattern.compile("^\\$?\\d+(?:\\.\\d{2})?$"));
        patterns.put("url", Pattern.compile("^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*$"));
        patterns.put("feature", Pattern.compile("^[\\w\\s.,()-]{3,200}$"));
        patterns.put("email", Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$"));
        patterns.put("phone", Pattern.compile("^\\+?\\d{10,15}$"));

        return patterns;
    }
}
