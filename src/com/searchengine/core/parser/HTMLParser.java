package com.searchengine.core.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {
    private static final String CSV_HEADER = "Model,SpeakerConfig,PowerOutput,AudioTechnology,Connectivity,WirelessFeatures,Price";
    private static final String CSV_DELIMITER = ",";
    
    // Enhanced patterns for better extraction
    private static final Pattern SPEAKER_CONFIG_PATTERN = Pattern.compile("(\\d\\.\\d(?:\\.\\d)?)|(?:([2-9]|\\d{2})\\s*(?:channel|ch))");
    private static final Pattern POWER_PATTERN = Pattern.compile("(\\d+(?:,\\d+)?(?:\\.\\d+)?).{0,5}(?:W|watts|Watts)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s*(\\d+(?:,\\d+)?(?:\\.\\d{2})?)");

    public void parseAndSaveToCSV(String htmlFolderPath, String outputCSVPath) {
        try {
            // Create/overwrite CSV file with header
            Files.write(
                Paths.get(outputCSVPath),
                (CSV_HEADER + "\n").getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );

            // Process each HTML file in the folder
            Files.walk(Paths.get(htmlFolderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".html"))
                .forEach(path -> processHTMLFile(path, outputCSVPath));
                
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private void processHTMLFile(Path htmlFile, String outputCSVPath) {
        try {
            // Read and parse HTML file
            Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");
            
            // Extract data with enhanced methods
            String model = extractModel(doc);
            String speakerConfig = extractSpeakerConfig(doc);
            String powerOutput = extractPowerOutput(doc);
            String audioTech = extractAudioTechnology(doc);
            String connectivity = extractConnectivity(doc);
            String wirelessFeatures = extractWirelessFeatures(doc);
            String price = extractPrice(doc);

            // Create CSV line
            String csvLine = String.join(CSV_DELIMITER,
                escapeCsvField(model),
                escapeCsvField(speakerConfig),
                escapeCsvField(powerOutput),
                escapeCsvField(audioTech),
                escapeCsvField(connectivity),
                escapeCsvField(wirelessFeatures),
                escapeCsvField(price)
            ) + "\n";

            // Append to CSV file
            Files.write(
                Paths.get(outputCSVPath),
                csvLine.getBytes(),
                StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.err.println("Error processing file " + htmlFile + ": " + e.getMessage());
        }
    }

    private String extractModel(Document doc) {
        // Enhanced model extraction
        String[] selectors = {
            "h1.product-name", 
            ".product-title", 
            ".model-number",
            "h1:contains(Soundbar)",
            "h1:contains(Theater)",
            "[itemprop=name]"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String model = elements.first().text().trim();
                // Clean up common marketing terms
                model = model.replaceAll("(?i)(buy|new|sale|price|review|best)", "").trim();
                return model;
            }
        }
        
        // Fallback to title if no other model info found
        return doc.title().replaceAll("(?i)(buy|new|sale|price|review|best)", "").trim();
    }

    private String extractSpeakerConfig(Document doc) {
        String fullText = doc.text();
        Matcher matcher = SPEAKER_CONFIG_PATTERN.matcher(fullText);
        
        if (matcher.find()) {
            String config = matcher.group();
            // Convert "7 channel" or "7ch" to "7.1" format if needed
            if (config.matches("\\d+\\s*(?:channel|ch)")) {
                String channels = config.replaceAll("\\D+", "");
                return channels + ".1";
            }
            return config;
        }
        return "";
    }

    private String extractPowerOutput(Document doc) {
        // Look in specific sections first
        Elements specSections = doc.select(".specifications, .specs, .technical-details");
        String textToSearch = !specSections.isEmpty() ? specSections.text() : doc.text();
        
        Matcher matcher = POWER_PATTERN.matcher(textToSearch);
        if (matcher.find()) {
            String power = matcher.group(1);
            // Clean up and standardize format
            power = power.replace(",", "");
            return power + "W";
        }
        return "";
    }

    private String extractAudioTechnology(Document doc) {
        Map<String, String> technologies = new LinkedHashMap<>();
        technologies.put("Dolby Atmos", "Dolby Atmos");
        technologies.put("DTS:X", "DTS:X");
        technologies.put("DTS-HD", "DTS-HD");
        technologies.put("DTS", "DTS");
        technologies.put("Dolby Digital Plus", "Dolby Digital Plus");
        technologies.put("Dolby Digital", "Dolby Digital");
        technologies.put("Dolby TrueHD", "Dolby TrueHD");
        
        List<String> found = new ArrayList<>();
        String text = doc.text().toLowerCase();
        
        for (Map.Entry<String, String> tech : technologies.entrySet()) {
            if (text.contains(tech.getKey().toLowerCase())) {
                found.add(tech.getValue());
            }
        }
        return String.join(";", found);
    }

    private String extractConnectivity(Document doc) {
        Map<String, List<String>> connectivityPatterns = new LinkedHashMap<>();
        connectivityPatterns.put("HDMI", Arrays.asList("HDMI", "hdmi"));
        connectivityPatterns.put("eARC", Arrays.asList("eARC", "earc"));
        connectivityPatterns.put("Bluetooth", Arrays.asList("Bluetooth", "bluetooth", "BT"));
        connectivityPatterns.put("Wi-Fi", Arrays.asList("Wi-Fi", "WiFi", "Wifi", "wifi"));
        connectivityPatterns.put("Optical", Arrays.asList("Optical", "optical", "TOSLINK"));
        connectivityPatterns.put("USB", Arrays.asList("USB", "usb"));
        
        Set<String> found = new LinkedHashSet<>(); // Using Set to avoid duplicates
        String text = doc.text();
        
        for (Map.Entry<String, List<String>> entry : connectivityPatterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (text.contains(pattern)) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }
        return String.join(";", found);
    }

    private String extractWirelessFeatures(Document doc) {
        Map<String, List<String>> wirelessPatterns = new LinkedHashMap<>();
        wirelessPatterns.put("wireless subwoofer", 
            Arrays.asList("wireless subwoofer", "wireless sub", "cordless subwoofer"));
        wirelessPatterns.put("wireless surround", 
            Arrays.asList("wireless surround", "wireless speakers", "wireless satellite"));
        wirelessPatterns.put("wireless rear", 
            Arrays.asList("wireless rear", "wireless back speakers", "wireless rear speakers"));
        
        Set<String> found = new LinkedHashSet<>();
        String text = doc.text().toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : wirelessPatterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (text.contains(pattern.toLowerCase())) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }
        return String.join(";", found);
    }

    private String extractPrice(Document doc) {
        // Look for price in specific sections first
        Elements priceElements = doc.select(".price, [itemprop=price], .product-price, .sale-price");
        String priceText = !priceElements.isEmpty() ? priceElements.text() : doc.text();
        
        Matcher matcher = PRICE_PATTERN.matcher(priceText);
        if (matcher.find()) {
            String price = matcher.group(1).replace(",", "");
            return "$" + price;
        }
        return "";
    }

    private String escapeCsvField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains delimiter or newline
        if (field.contains("\"") || field.contains(CSV_DELIMITER) || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    // Main method for testing
    public static void main(String[] args) {
        HTMLParser parser = new HTMLParser();
        String htmlFolder = "crawled_pages";
        String outputCsv = "speaker_systems.csv";
        parser.parseAndSaveToCSV(htmlFolder, outputCsv);
    }
}