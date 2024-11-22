package com.searchengine.core.spell;

import com.searchengine.model.Product;
import java.util.*;

public class SpellChecker {
    private Set<String> vocabulary;
    private Map<Integer, Set<String>> wordsByLength;
    private static final int MAX_EDIT_DISTANCE = 2;
    private static final int MAX_SUGGESTIONS = 5;

    public SpellChecker() {
        this.vocabulary = new HashSet<>();
        this.wordsByLength = new HashMap<>();
    }

    public void buildVocabulary(List<Product> products) {
        System.out.println("Building vocabulary from products...");

        for (Product product : products) {
            // Add words from product name
            addWordsToVocabulary(product.getName());

            // Add words from features
            for (String feature : product.getFeatures()) {
                addWordsToVocabulary(feature);
            }

            // Add words from description
            addWordsToVocabulary(product.getDescription());

            // Add words from specifications
            for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
                addWordsToVocabulary(spec.getKey());
                addWordsToVocabulary(spec.getValue());
            }

            // Add category
            addWordsToVocabulary(product.getCategory());
        }

        System.out.println("Vocabulary built with " + vocabulary.size() + " words");
        System.out.println("Word length distribution: ");
        wordsByLength.forEach((length, words) ->
                System.out.println("Length " + length + ": " + words.size() + " words"));
    }

    private void addWordsToVocabulary(String text) {
        if (text == null) return;

        // Split text into words, remove special characters
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .split("\\s+");

        for (String word : words) {
            // Skip very short words and numbers
            if (word.length() <= 2 || word.matches("\\d+")) continue;

            // Add to main vocabulary
            vocabulary.add(word);

            // Add to length-based index
            wordsByLength
                    .computeIfAbsent(word.length(), k -> new HashSet<>())
                    .add(word);
        }
    }

    public boolean isWordValid(String word) {
        if (word == null || word.isEmpty()) return false;
        return vocabulary.contains(word.toLowerCase());
    }

    public List<String> getSuggestions(String word) {
        if (word == null || word.isEmpty()) return Collections.emptyList();

        word = word.toLowerCase();
        PriorityQueue<ScoredWord> suggestions = new PriorityQueue<>();

        // Check words of similar length
        int wordLength = word.length();
        for (int length = wordLength - 1; length <= wordLength + 1; length++) {
            Set<String> candidateWords = wordsByLength.getOrDefault(length, Collections.emptySet());

            for (String candidate : candidateWords) {
                int distance = calculateEditDistance(word, candidate);
                if (distance <= MAX_EDIT_DISTANCE) {
                    suggestions.offer(new ScoredWord(candidate, distance));
                }
            }
        }

        // Extract top suggestions
        List<String> result = new ArrayList<>();
        while (!suggestions.isEmpty() && result.size() < MAX_SUGGESTIONS) {
            result.add(suggestions.poll().word);
        }

        return result;
    }

    private int calculateEditDistance(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];

        // Initialize first row
        for (int j = 0; j <= s2.length(); j++) {
            prev[j] = j;
        }

        // Calculate edit distance
        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    curr[j] = prev[j - 1];
                } else {
                    curr[j] = 1 + Math.min(
                            Math.min(prev[j],     // deletion
                                    curr[j - 1]), // insertion
                            prev[j - 1]           // substitution
                    );
                }
            }
            // Swap arrays
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[s2.length()];
    }

    // Helper class for ranking suggestions
    private static class ScoredWord implements Comparable<ScoredWord> {
        final String word;
        final int score;

        ScoredWord(String word, int score) {
            this.word = word;
            this.score = score;
        }

        @Override
        public int compareTo(ScoredWord other) {
            return Integer.compare(this.score, other.score);
        }
    }

    // Test the spell checker
    public static void main(String[] args) {
        SpellChecker spellChecker = new SpellChecker();

        // Create some test products
        List<Product> testProducts = new ArrayList<>();

        Product p1 = new Product();
        p1.setName("Wireless Soundbar");
        p1.addFeature("Bluetooth Connectivity");
        p1.addFeature("Dolby Atmos Support");
        p1.setDescription("High-quality sound system for home theater");

        Product p2 = new Product();
        p2.setName("Subwoofer Speaker");
        p2.addFeature("Deep Bass Performance");
        p2.addFeature("Wireless Connection");
        p2.setDescription("Powerful bass enhancement for your audio system");

        testProducts.add(p1);
        testProducts.add(p2);

        // Build vocabulary
        spellChecker.buildVocabulary(testProducts);

        // Test some misspellings
        String[] testWords = {
                "wireless",    // correct
                "wirless",    // misspelled
                "soundbar",   // correct
                "soundbarr",  // misspelled
                "blutooth",   // misspelled
                "speakar"     // misspelled
        };

        for (String word : testWords) {
            System.out.println("\nTesting word: " + word);
            System.out.println("Valid: " + spellChecker.isWordValid(word));
            if (!spellChecker.isWordValid(word)) {
                System.out.println("Suggestions: " + spellChecker.getSuggestions(word));
            }
        }
    }
}