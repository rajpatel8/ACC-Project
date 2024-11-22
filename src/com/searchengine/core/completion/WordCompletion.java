package com.searchengine.core.completion;

import com.searchengine.model.Product;
import java.util.*;

public class WordCompletion {
    private final TrieNode root;
    private static final int MAX_SUGGESTIONS = 10;
    private static final int MIN_WORD_LENGTH = 3;

    public WordCompletion() {
        this.root = new TrieNode();
    }

    public void buildTrie(List<Product> products) {
        System.out.println("Building Trie from products...");

        for (Product product : products) {
            // Add words from product name
            addWordsToTrie(product.getName(), product.getProductId());

            // Add words from features
            for (String feature : product.getFeatures()) {
                addWordsToTrie(feature, product.getProductId());
            }

            // Add words from description
            addWordsToTrie(product.getDescription(), product.getProductId());

            // Add words from specifications
            for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                addWordsToTrie(spec.getValue(), product.getProductId());
            }

            // Add category
            addWordsToTrie(product.getCategory(), product.getProductId());
        }

        System.out.println("Trie construction completed");
    }

    private void addWordsToTrie(String text, String productId) {
        if (text == null) return;

        // Split text into words and clean
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() >= MIN_WORD_LENGTH) {
                insertWord(word, productId);
            }
        }
    }

    private void insertWord(String word, String productId) {
        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            current.getChildren()
                    .putIfAbsent(ch, new TrieNode());
            current = current.getChildren().get(ch);
            current.addProductId(productId);  // Track product ID at each node
        }

        current.setEndOfWord(true);
        current.incrementFrequency();
    }

    public List<Suggestion> getSuggestions(String prefix) {
        List<Suggestion> suggestions = new ArrayList<>();

        // Convert prefix to lowercase
        prefix = prefix.toLowerCase();

        // Find node corresponding to prefix
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            if (!current.getChildren().containsKey(ch)) {
                return suggestions;  // No suggestions if prefix not found
            }
            current = current.getChildren().get(ch);
        }

        // Find all words with this prefix
        findAllWords(current, prefix, suggestions);

        // Sort suggestions by frequency
        suggestions.sort((s1, s2) -> Integer.compare(s2.getFrequency(), s1.getFrequency()));

        // Return top suggestions
        return suggestions.subList(0, Math.min(suggestions.size(), MAX_SUGGESTIONS));
    }

    private void findAllWords(TrieNode node, String prefix, List<Suggestion> suggestions) {
        if (node.isEndOfWord()) {
            suggestions.add(new Suggestion(
                    prefix,
                    node.getFrequency(),
                    node.getProductIds()
            ));
        }

        for (Map.Entry<Character, TrieNode> child : node.getChildren().entrySet()) {
            findAllWords(child.getValue(), prefix + child.getKey(), suggestions);
        }
    }

    public static class Suggestion {
        private final String word;
        private final int frequency;
        private final Set<String> productIds;

        public Suggestion(String word, int frequency, Set<String> productIds) {
            this.word = word;
            this.frequency = frequency;
            this.productIds = new HashSet<>(productIds);
        }

        public String getWord() { return word; }
        public int getFrequency() { return frequency; }
        public Set<String> getProductIds() { return productIds; }

        @Override
        public String toString() {
            return String.format("%s (freq: %d, products: %d)",
                    word, frequency, productIds.size());
        }
    }

    // Test the word completion
    public static void main(String[] args) {
        WordCompletion wordCompletion = new WordCompletion();

        // Create test products
        List<Product> testProducts = new ArrayList<>();

        Product p1 = new Product();
        p1.setProductId("sb001");
        p1.setName("Wireless Soundbar");
        p1.addFeature("Bluetooth Connectivity");
        p1.setDescription("Premium soundbar with wireless features");

        Product p2 = new Product();
        p2.setProductId("sb002");
        p2.setName("Soundbar with Subwoofer");
        p2.addFeature("Wireless Subwoofer");
        p2.setDescription("Complete sound system with wireless subwoofer");

        testProducts.add(p1);
        testProducts.add(p2);

        // Build Trie
        wordCompletion.buildTrie(testProducts);

        // Test some prefixes
        String[] testPrefixes = {"sou", "wire", "blu", "sub"};

        for (String prefix : testPrefixes) {
            System.out.println("\nSuggestions for prefix '" + prefix + "':");
            List<Suggestion> suggestions = wordCompletion.getSuggestions(prefix);
            for (Suggestion suggestion : suggestions) {
                System.out.println("- " + suggestion);
            }
        }
    }
}