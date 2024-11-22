package com.searchengine.core.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {
    private final Gson gson;
    
    // Enhanced patterns for better extraction
    private static final Pattern SPEAKER_CONFIG_PATTERN = Pattern.compile("(\\d\\.\\d(?:\\.\\d)?)|(?:([2-9]|\\d{2})\\s*(?:channel|ch))");
    private static final Pattern POWER_PATTERN = Pattern.compile("(\\d+(?:,\\d+)?(?:\\.\\d+)?).{0,5}(?:W|watts|Watts)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s*(\\d+(?:,\\d+)?(?:\\.\\d{2})?)");

    // Audio technologies map
    private static final Map<String, String> AUDIO_TECHNOLOGIES = new LinkedHashMap<>() {{
        put("Dolby Atmos", "Dolby Atmos");
        put("DTS:X", "DTS:X");
        put("DTS-HD", "DTS-HD");
        put("DTS", "DTS");
        put("Dolby Digital Plus", "Dolby Digital Plus");
        put("Dolby Digital", "Dolby Digital");
        put("Dolby TrueHD", "Dolby TrueHD");
        put("Trueplay", "Trueplay");
    }};

    // Connectivity patterns
    private static final Map<String, List<String>> CONNECTIVITY_PATTERNS = new LinkedHashMap<>() {{
        put("HDMI", Arrays.asList("HDMI", "hdmi"));
        put("eARC", Arrays.asList("eARC", "earc"));
        put("Bluetooth", Arrays.asList("Bluetooth", "bluetooth", "BT"));
        put("Wi-Fi", Arrays.asList("Wi-Fi", "WiFi", "Wifi", "wifi"));
        put("Optical", Arrays.asList("Optical", "optical", "TOSLINK"));
        put("USB", Arrays.asList("USB", "usb"));
        put("AirPlay", Arrays.asList("AirPlay", "airplay"));    
        put("Ethernet", Arrays.asList("Ethernet", "ethernet")); 
    }};

    public HTMLParser() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void parseHtmlFiles(String htmlDirectory, String outputJsonPath) {
        try {
            Map<String, Map<String, Object>> result = new HashMap<>();
            result.put("company", new HashMap<>());
            Map<String, Object> companies = result.get("company");

            Files.walk(Paths.get(htmlDirectory))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".html"))
                .forEach(path -> {
                    try {
                        System.out.println("Processing: " + path.getFileName());
                        Document doc = Jsoup.parse(path.toFile(), "UTF-8");
                        String company = detectCompany(path.getFileName().toString(), doc);
                        
                        if (company != null) {
                            List<Map<String, Object>> products = parseProducts(doc, company);
                            if (!products.isEmpty()) {
                                Map<String, Object> companyData;
                                if (companies.containsKey(company)) {
                                    companyData = (Map<String, Object>) companies.get(company);
                                } else {
                                    companyData = new HashMap<>();
                                    companies.put(company, companyData);
                                }
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> existingProducts = 
                                    (List<Map<String, Object>>) companyData.computeIfAbsent("products", k -> new ArrayList<>());
                                existingProducts.addAll(products);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing file: " + path);
                        e.printStackTrace();
                    }
                });

            Files.writeString(Paths.get(outputJsonPath), gson.toJson(result));
            System.out.println("JSON output written to: " + outputJsonPath);

        } catch (IOException e) {
            System.err.println("Error in file processing");
            e.printStackTrace();
        }
    }


    private String detectCompany(String filename, Document doc) {
        String content = doc.text().toLowerCase();
        filename = filename.toLowerCase();
        
        if (content.contains("jbl") || filename.contains("jbl")) return "JBL";
        if (content.contains("bose") || filename.contains("bose")) return "Bose";
        if (content.contains("sony") || filename.contains("sony")) return "Sony";
        if (content.contains("lg electronics") || filename.contains("lg")) return "LG";
        if (content.contains("samsung") || filename.contains("samsung")) return "Samsung";
        if (content.contains("Sonos") || filename.contains("sonos")) return "Sonos";
        
        return null;
    }

    private List<Map<String, Object>> parseProducts(Document doc, String company) {
        List<Map<String, Object>> products = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();

        // Try different product container selectors
        String[] selectors = {
            "div[class*=product]", ".product-tile", ".product-card",
            "div[class*=speaker]", "div[class*=audio]", ".item-card"
        };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                if (isValidProductElement(element)) {
                    Map<String, Object> product = extractProductData(element);
                    if (isValidProduct(product, processedNames)) {
                        products.add(product);
                        processedNames.add((String) product.get("name"));
                    }
                }
            }
        }

        return products;
    }

    private boolean isValidProductElement(Element element) {
        String text = element.text().toLowerCase();
        return text.contains("speaker") || text.contains("soundbar") || 
               text.contains("subwoofer") || text.contains("theater") ||
               text.contains("audio") && !isNavigationElement(element);
    }

    private boolean isNavigationElement(Element element) {
        return element.hasClass("nav") || element.hasClass("menu") ||
               element.hasClass("header") || element.hasClass("footer") ||
               element.tagName().equals("nav");
    }

    private Map<String, Object> extractProductData(Element element) {
        Map<String, Object> product = new HashMap<>();
        
        // Extract model/name
        String name = extractModel(element);
        if (name == null || name.isEmpty()) {
            return null;
        }
        product.put("name", name);

        // Extract specifications
        String speakerConfig = extractSpeakerConfig(element);
        if (!speakerConfig.isEmpty()) {
            product.put("speakerConfiguration", speakerConfig);
        }

        // Extract power output
        String powerOutput = extractPowerOutput(element);
        if (!powerOutput.isEmpty()) {
            product.put("powerOutput", powerOutput);
        }

        // Extract audio technologies
        List<String> audioTech = extractAudioTechnology(element);
        if (!audioTech.isEmpty()) {
            product.put("audioTechnology", audioTech);
        }

        // Extract connectivity
        List<String> connectivity = extractConnectivity(element);
        if (!connectivity.isEmpty()) {
            product.put("connectivity", connectivity);
        }

        // Extract price
        double price = extractPrice(element);
        if (price > 0) {
            product.put("price", price);
        }

        // Determine product type
        product.put("type", determineProductType(name, element.text()));

        return product;
    }

    private String extractModel(Element element) {
        String[] selectors = {
            "h1.product-name", ".product-title", ".model-number",
            "[itemprop=name]", ".name", "h1", "h2", "h3"
        };
        
        for (String selector : selectors) {
            Elements elements = element.select(selector);
            if (!elements.isEmpty()) {
                String model = elements.first().text().trim();
                model = model.replaceAll("(?i)(buy|new|sale|price|review|best)", "").trim();
                if (isValidProductName(model)) {
                    return model;
                }
            }
        }
        return null;
    }

    // Rest of the extraction methods from your original implementation...
    private String extractSpeakerConfig(Element element) {
        String text = element.text();
        Matcher matcher = SPEAKER_CONFIG_PATTERN.matcher(text);
        
        if (matcher.find()) {
            String config = matcher.group();
            
            // Convert channel format to x.y format
            if (config.matches("\\d+\\s*(?:channel|ch)")) {
                String channels = config.replaceAll("\\D+", "");
                return channels + ".1";
            }
            
            // Validate the configuration format
            if (config.matches("\\d\\.\\d(?:\\.\\d)?")) {
                String[] parts = config.split("\\.");
                int mainChannels = Integer.parseInt(parts[0]);
                int subChannels = Integer.parseInt(parts[1]);
                
                // Validate reasonable channel numbers
                if (mainChannels >= 1 && mainChannels <= 11 && 
                    subChannels >= 0 && subChannels <= 4) {
                    return config;
                }
            }
        }
        return "";
    }

    private String extractPowerOutput(Element element) {
        Matcher matcher = POWER_PATTERN.matcher(element.text());
        if (matcher.find()) {
            String power = matcher.group(1).replace(",", "");
            try {
                int watts = Integer.parseInt(power);
                // Filter out unreasonable power values
                if (watts > 0 && watts <= 2000) {
                    return watts + "W";
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
        return "";
    }

    private List<String> extractAudioTechnology(Element element) {
        List<String> found = new ArrayList<>();
        String text = element.text().toLowerCase();
        
        for (Map.Entry<String, String> tech : AUDIO_TECHNOLOGIES.entrySet()) {
            if (text.contains(tech.getKey().toLowerCase())) {
                found.add(tech.getValue());
            }
        }
        return found;
    }

    private List<String> extractConnectivity(Element element) {
        List<String> found = new ArrayList<>();
        String text = element.text();
        
        for (Map.Entry<String, List<String>> entry : CONNECTIVITY_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (text.contains(pattern)) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }
        return found;
    }

    private double extractPrice(Element element) {
        Elements priceElements = element.select(".price, [itemprop=price], .product-price, .sale-price");
        String priceText = !priceElements.isEmpty() ? priceElements.text() : element.text();
        
        Matcher matcher = PRICE_PATTERN.matcher(priceText);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private boolean isValidProductName(String name) {
        if (name == null || name.isEmpty() || name.length() > 100) {
            return false;
        }
        
        String lower = name.toLowerCase();
        
        // Filter out common non-product text
        String[] invalidPatterns = {
            "filter", "explore", "shop", "search", "model number",
            "delivery", "services", "general", "high definition",
            "your", "master", "powerful", "stream", "enhance",
            "black friday", "holiday", "offers", "features",
            "smart home", "voice control", "group" 
        };
        
        for (String pattern : invalidPatterns) {
            if (lower.contains(pattern)) {
                return false;
            }
        }
        
        // Must contain model identifiers or brand names
        boolean hasModelIdentifier = lower.matches(".*(?:model|[a-z]-?[0-9]|[0-9](?:ch|w)).*") ||
                                   lower.contains("soundbar") ||
                                   lower.contains("speaker") ||
                                   lower.contains("theater");
        
        return hasModelIdentifier;
    }

    private String determineProductType(String name, String description) {
        String searchText = (name + " " + description).toLowerCase();
        
        // More specific type detection
        if (searchText.contains("subwoofer") || searchText.contains("sub")) return "Subwoofer";
        if (searchText.contains("soundbar")) return "Soundbar";
        if (searchText.matches(".*\\bcenter\\b.*speaker.*") || 
            searchText.matches(".*\\bcentre\\b.*speaker.*")) return "Center Speaker";
        if (searchText.matches(".*\\bsurround\\b.*speaker.*") || 
            searchText.contains("satellite")) return "Surround Speaker";
        if (searchText.contains("receiver") && !searchText.contains("soundbar")) return "Receiver";
        if (searchText.contains("bookshelf") || 
            searchText.contains("floor-standing") || 
            searchText.contains("tower")) return "Speaker System";
        
        // Default based on channel configuration
        if (searchText.matches(".*\\b[5-7]\\.1.*")) return "Surround Speaker";
        if (searchText.matches(".*\\b2\\.1.*")) return "Speaker System";
        
        return "Speaker System";
    }

    private boolean isValidProduct(Map<String, Object> product, Set<String> processedNames) {
    if (product == null || !product.containsKey("name")) {
        return false;
    }

    String name = (String) product.get("name");
    
    // Additional validation
    if (name.length() < 3 || name.length() > 100) {
        return false;
    }
    
    if (processedNames.contains(name)) {
        return false;
    }
    
    String nameLower = name.toLowerCase();
    
    // Must look like a product name
    boolean validFormat = nameLower.matches(".*(?:[a-z]-?[0-9]|[0-9](?:ch|w)).*") ||
                         nameLower.contains("soundbar") ||
                         nameLower.contains("speaker") ||
                         nameLower.contains("subwoofer");
    
    // Must not be a general category or marketing text
    boolean isNotGeneric = !nameLower.matches(".*(category|products|accessories|audio|sound)\\s*$");
    
    return validFormat && isNotGeneric && isValidProductName(name);
}

    public static void main(String[] args) {
        HTMLParser parser = new HTMLParser();
        parser.parseHtmlFiles("html_pages", "audio_products.json");
    }
}