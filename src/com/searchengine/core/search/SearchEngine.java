package com.searchengine.core.search;

import com.searchengine.core.*;
import com.searchengine.core.crawler.*;
import com.searchengine.core.parser.HTMLParser;
import com.searchengine.core.spell.SpellChecker;
import com.searchengine.core.completion.WordCompletion;
import com.searchengine.core.frequency.FrequencyAnalyzer;
import com.searchengine.core.ranking.PageRanker;
import com.searchengine.core.indexing.InvertedIndex;
import com.searchengine.core.patterns.PatternMatcher;
import com.searchengine.model.Product;
import java.util.*;
import java.util.concurrent.*;

public class SearchEngine {
    private final WebCrawler crawler;
    private final HTMLParser parser;
    private final SpellChecker spellChecker;
    private final WordCompletion wordCompletion;
    private final FrequencyAnalyzer frequencyAnalyzer;
    private final PageRanker pageRanker;
    private final InvertedIndex invertedIndex;
    private final PatternMatcher patternMatcher;
    private final List<Product> products;
    private final ExecutorService executorService;

    public SearchEngine() {


        CrawlerConfig config = new CrawlerConfig();
        config.setMaxDepth(3);
        config.setMaxPages(100);

        this.crawler = new WebCrawler(config);
        this.parser = new HTMLParser();
        this.spellChecker = new SpellChecker();
        this.wordCompletion = new WordCompletion();
        this.frequencyAnalyzer = new FrequencyAnalyzer();
        this.pageRanker = new PageRanker(frequencyAnalyzer);
        this.invertedIndex = new InvertedIndex();
        this.patternMatcher = new PatternMatcher();
        this.products = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void initialize() {
        CompletableFuture.allOf(
                CompletableFuture.runAsync(this::initializeSpellChecker),
                CompletableFuture.runAsync(this::initializeWordCompletion),
                CompletableFuture.runAsync(this::initializeInvertedIndex),
                CompletableFuture.runAsync(this::initializeFrequencyAnalyzer)
        ).join();
    }

    private void initializeSpellChecker() {
        System.out.println("Initializing Spell Checker...");
        spellChecker.buildVocabulary(products);
    }

    private void initializeWordCompletion() {
        System.out.println("Initializing Word Completion...");
        wordCompletion.buildTrie(products);
    }

    private void initializeInvertedIndex() {
        System.out.println("Initializing Inverted Index...");
        invertedIndex.buildIndex(products);
    }

    private void initializeFrequencyAnalyzer() {
        System.out.println("Initializing Frequency Analyzer...");
        frequencyAnalyzer.analyzeProducts(products);
    }

    public SearchResult search(String query) {
        SearchResult result = new SearchResult();

        // Check spelling
        if (!spellChecker.isWordValid(query)) {
            result.addSpellingSuggestions(spellChecker.getSuggestions(query));
        }

        // Get word completions
        result.setCompletions(wordCompletion.getSuggestions(query));

        var indexResults = invertedIndex.search(query);
        result.setIndexResults(indexResults.getItems());

        // Apply enhanced page ranking
        var rankedResults = pageRanker.rankProducts(
                indexResults.getItems().stream()
                        .map(item -> item.getProduct())
                        .toList(),
                query
        );
        result.setRankedResults(rankedResults);

        // Record search frequency
        frequencyAnalyzer.recordSearch(query);

        return result;
    }

    public void addProducts(List<Product> newProducts) {
        products.addAll(newProducts);
        initialize();
    }

    // Getters for components
    public List<Product> getProducts() { return Collections.unmodifiableList(products); }
    public SpellChecker getSpellChecker() { return spellChecker; }
    public WordCompletion getWordCompletion() { return wordCompletion; }
    public FrequencyAnalyzer getFrequencyAnalyzer() { return frequencyAnalyzer; }
    public PatternMatcher getPatternMatcher() { return patternMatcher; }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}