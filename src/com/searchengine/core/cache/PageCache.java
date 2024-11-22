package com.searchengine.core.cache;

import com.searchengine.model.WebPage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PageCache {
    private final String cacheDirectory;
    private final long cacheExpirationMs;
    private final Map<String, CacheEntry> memoryCache;
    private final Gson gson;

    public PageCache(String cacheDirectory, long cacheExpirationHours) {
        this.cacheDirectory = cacheDirectory;
        this.cacheExpirationMs = cacheExpirationHours * 3600000; // Convert hours to milliseconds
        this.memoryCache = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        initializeCache();
    }

    private void initializeCache() {
        try {
            // Create cache directory if it doesn't exist
            Path cachePath = Paths.get(cacheDirectory);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }

            // Load existing cache entries
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(cachePath, "*.cache")) {
                for (Path file : stream) {
                    try {
                        String content = Files.readString(file);
                        CacheEntry entry = gson.fromJson(content, CacheEntry.class);
                        if (!isExpired(entry)) {
                            String url = file.getFileName().toString().replace(".cache", "");
                            memoryCache.put(url, entry);
                        } else {
                            Files.delete(file);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading cache file: " + file);
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Cache initialized with " + memoryCache.size() + " valid entries");

        } catch (IOException e) {
            System.err.println("Error initializing cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasValidCache(String url) {
        String urlHash = hashUrl(url);
        CacheEntry entry = memoryCache.get(urlHash);
        return entry != null && !isExpired(entry);
    }

    public WebPage getFromCache(String url) {
        String urlHash = hashUrl(url);
        CacheEntry entry = memoryCache.get(urlHash);
        if (entry != null && !isExpired(entry)) {
            return entry.getPage();
        }
        return null;
    }

    public void addToCache(WebPage page) {
        try {
            String urlHash = hashUrl(page.getUrl());
            CacheEntry entry = new CacheEntry(page);

            // Add to memory cache
            memoryCache.put(urlHash, entry);

            // Save to file
            String cacheFileName = urlHash + ".cache";
            Path cachePath = Paths.get(cacheDirectory, cacheFileName);
            String jsonContent = gson.toJson(entry);
            Files.writeString(cachePath, jsonContent);

        } catch (Exception e) {
            System.err.println("Error caching page: " + page.getUrl());
            e.printStackTrace();
        }
    }

    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.getTimestamp() > cacheExpirationMs;
    }

    public void clearCache() {
        try {
            memoryCache.clear();
            Files.walk(Paths.get(cacheDirectory))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cache"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Error deleting cache file: " + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error clearing cache: " + e.getMessage());
        }
    }

    private String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // If hashing fails, use a simple fallback
            return url.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

    // Inner class to represent a cache entry
    private static class CacheEntry implements Serializable {
        private final WebPage page;
        private final long timestamp;

        public CacheEntry(WebPage page) {
            this.page = page;
            this.timestamp = System.currentTimeMillis();
        }

        public WebPage getPage() { return page; }
        public long getTimestamp() { return timestamp; }
    }
}