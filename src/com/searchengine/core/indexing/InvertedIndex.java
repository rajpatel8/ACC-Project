package com.searchengine.core.indexing;

import com.searchengine.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class InvertedIndex {
    private final Map<String, IndexEntry> index;
    private final Map<String, ProductDocument> documents;
    private final Set<String> stopWords;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
        this.documents = new ConcurrentHashMap<>();
        this.stopWords = initializeStopWords();
    }

    private Set<String> initializeStopWords() {
        return new HashSet<>(Arrays.asList(
                "the", "and", "or", "a", "an", "in", "on", "at", "to", "for",
                "with", "by", "from", "up", "about", "into", "over", "after"
        ));
    }

    public void buildIndex(List<Product> products) {
        System.out.println("Building inverted index...");

        for (Product product : products) {
            indexProduct(product);
        }

        // Print statistics
        System.out.println("Index built successfully:");
        System.out.println("- Total unique terms: " + index.size());
        System.out.println("- Total documents: " + documents.size());

        // Print most common terms
        System.out.println("\nMost common terms:");
        index.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                        e2.getValue().getDocumentFrequency(),
                        e1.getValue().getDocumentFrequency()))
                .limit(10)
                .forEach(e -> System.out.println("- " + e.getKey() + ": " +
                        e.getValue().getDocumentFrequency() + " documents"));
    }

    private void indexProduct(Product product) {
        // Create document representation
        ProductDocument doc = new ProductDocument(product);
        documents.put(product.getProductId(), doc);

        // Index each field
        indexField(product.getName(), product.getProductId(), 2.0); // Higher weight for name
        indexField(product.getDescription(), product.getProductId(), 1.0);

        for (String feature : product.getFeatures()) {
            indexField(feature, product.getProductId(), 1.5); // Higher weight for features
        }

        for (Map.Entry<String, String> spec : product.getSpecifications().entrySet()) {
            indexField(spec.getKey() + " " + spec.getValue(), product.getProductId(), 1.0);
        }

        indexField(product.getCategory(), product.getProductId(), 1.2); // Higher weight for category
    }

    private void indexField(String text, String productId, double weight) {
        if (text == null) return;

        // Tokenize and normalize text
        List<String> terms = tokenize(text);

        // Update document terms
        ProductDocument doc = documents.get(productId);

        for (String term : terms) {
            // Skip stop words and very short terms
            if (stopWords.contains(term) || term.length() <= 2) continue;

            // Update document term frequency
            doc.addTerm(term, weight);

            // Update inverted index
            index.computeIfAbsent(term, k -> new IndexEntry())
                    .addOccurrence(productId, weight);
        }
    }

    private List<String> tokenize(String text) {
        return Arrays.asList(text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .trim()
                .split("\\s+"));
    }

    public SearchResult search(String query) {
        // Tokenize query
        List<String> queryTerms = tokenize(query);

        // Calculate scores for each document
        Map<String, Double> scores = new HashMap<>();
        Map<String, Set<String>> matchedTerms = new HashMap<>();

        for (String term : queryTerms) {
            if (stopWords.contains(term)) continue;

            IndexEntry entry = index.get(term);
            if (entry != null) {
                double idf = calculateIDF(entry.getDocumentFrequency());

                for (Map.Entry<String, PostingList> posting : entry.getPostings().entrySet()) {
                    String productId = posting.getKey();
                    double tf = posting.getValue().getTermFrequency();

                    // TF-IDF scoring
                    double score = tf * idf * posting.getValue().getWeight();
                    scores.merge(productId, score, Double::sum);

                    // Track matched terms
                    matchedTerms.computeIfAbsent(productId, k -> new HashSet<>())
                            .add(term);
                }
            }
        }

        // Create search results
        List<SearchResultItem> results = new ArrayList<>();
        for (Map.Entry<String, Double> score : scores.entrySet()) {
            String productId = score.getKey();
            ProductDocument doc = documents.get(productId);

            results.add(new SearchResultItem(
                    doc.getProduct(),
                    score.getValue(),
                    matchedTerms.get(productId)
            ));
        }

        // Sort results by score
        results.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        return new SearchResult(results, Instant.now());
    }

    private double calculateIDF(int documentFrequency) {
        return Math.log(1.0 + ((double) documents.size() / documentFrequency));
    }
}