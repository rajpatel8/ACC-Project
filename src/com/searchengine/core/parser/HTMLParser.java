// File: src/com/searchengine/core/parser/HTMLParser.java
package com.searchengine.core.parser;

import com.searchengine.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLParser {
    // Regular expressions for price extraction
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s*(\\d+(?:,\\d+)?(?:\\.\\d{2})?)");

    // Company-specific selectors
    private static final Map<String, CompanySelectors> COMPANY_SELECTORS = new HashMap<>() {{
        put("BOSE", new CompanySelectors(
                ".product-tile",
                "[data-testid='product-name']",
                ".product-price",
                ".product-features li",
                ".product-description"
        ));

        put("SONY", new CompanySelectors(
                ".product-item",
                ".product-name",
                ".price",
                ".features li",
                ".description"
        ));

        put("LG", new CompanySelectors(
                ".product-card",
                ".model-name",
                ".price-info",
                ".feature-list li",
                ".prod-description"
        ));

        put("SAMSUNG", new CompanySelectors(
                ".product-card",
                ".product-name",
                ".product-price",
                ".feature-list li",
                ".product-description"
        ));

        put("JBL", new CompanySelectors(
                ".product-tile",
                ".product-name",
                ".product-price",
                ".feature-item",
                ".product-description"
        ));
    }};

    public static List<Product> extractProducts(String html, String company) {
        List<Product> products = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(html);
            CompanySelectors selectors = COMPANY_SELECTORS.getOrDefault(
                    company.toUpperCase(),
                    new CompanySelectors("", "", "", "", "")
            );

            // Find all product containers
            Elements productElements = doc.select(selectors.productContainer);

            System.out.println("Found " + productElements.size() + " product elements for " + company);

            for (Element productElement : productElements) {
                try {
                    Product product = extractProduct(productElement, selectors, company);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    System.err.println("Error extracting product: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing HTML: " + e.getMessage());
        }

        return products;
    }

    private static Product extractProduct(Element element, CompanySelectors selectors, String company) {
        Product product = new Product();

        try {
            // Extract product name
            String name = element.select(selectors.nameSelector).text();
            if (name.isEmpty()) {
                return null; // Skip if no name found
            }
            product.setName(name);

            // Extract price
            String priceText = element.select(selectors.priceSelector).text();
            double price = extractPrice(priceText);
            product.setPrice(price);

            // Extract features
            Elements features = element.select(selectors.featureSelector);
            for (Element feature : features) {
                String featureText = feature.text().trim();
                if (!featureText.isEmpty()) {
                    product.addFeature(featureText);
                }
            }

            // Extract description
            String description = element.select(selectors.descriptionSelector).text();
            product.setDescription(description);

            // Set company-specific data
            product.setCategory(company);

            // Generate product ID
            String productId = generateProductId(company, name);
            product.setProductId(productId);

            // Extract URL if available
            Element link = element.selectFirst("a[href]");
            if (link != null) {
                product.setUrl(link.attr("abs:href"));
            }

            // Extract images if available
            Elements images = element.select("img[src]");
            for (Element img : images) {
                String src = img.attr("abs:src");
                if (!src.isEmpty()) {
                    product.addImage(src);
                }
            }

            // Additional specifications
            Elements specs = element.select("dl.specifications dt, dl.specifications dd");
            for (int i = 0; i < specs.size() - 1; i += 2) {
                String key = specs.get(i).text().trim();
                String value = specs.get(i + 1).text().trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    product.addSpecification(key, value);
                }
            }

            return product;

        } catch (Exception e) {
            System.err.println("Error processing product element: " + e.getMessage());
            return null;
        }
    }

    private static double extractPrice(String priceText) {
        try {
            Matcher matcher = PRICE_PATTERN.matcher(priceText);
            if (matcher.find()) {
                String price = matcher.group(1).replace(",", "");
                return Double.parseDouble(price);
            }
        } catch (Exception e) {
            System.err.println("Error parsing price: " + priceText);
        }
        return 0.0;
    }

    private static String generateProductId(String company, String name) {
        return (company + "_" + name)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    // Helper class for company-specific CSS selectors
    private static class CompanySelectors {
        final String productContainer;
        final String nameSelector;
        final String priceSelector;
        final String featureSelector;
        final String descriptionSelector;

        CompanySelectors(String productContainer, String nameSelector,
                         String priceSelector, String featureSelector,
                         String descriptionSelector) {
            this.productContainer = productContainer;
            this.nameSelector = nameSelector;
            this.priceSelector = priceSelector;
            this.featureSelector = featureSelector;
            this.descriptionSelector = descriptionSelector;
        }
        public void parseAndSaveToCSV(String html, String company, String outputCSV) {
            List<Product> products = extractProducts(html, company);

            try (FileWriter writer = new FileWriter(outputCSV)) {
                // Write CSV header
                writer.write("Product ID,Name,Price,Features,Description,Category,URL,Images,Specifications\n");

                // Write product data
                for (Product product : products) {
                    String features = String.join("|", product.getFeatures());
                    String images = String.join("|", product.getImages());
                    String specifications = formatSpecifications(product.getSpecifications());

                    writer.write(String.format("%s,\"%s\",%f,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            product.getProductId(),
                            escapeCSV(product.getName()),
                            product.getPrice(),
                            escapeCSV(features),
                            escapeCSV(product.getDescription()),
                            product.getCategory(),
                            product.getUrl(),
                            escapeCSV(images),
                            escapeCSV(specifications)
                    ));
                }

                System.out.println("Product data saved to " + outputCSV);

            } catch (IOException e) {
                System.err.println("Error writing to CSV: " + e.getMessage());
            }
        }

        private String formatSpecifications(Map<String, String> specifications) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : specifications.entrySet()) {
                sb.append(entry.getKey()).append(":").append(entry.getValue()).append("|");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // Remove trailing "|"
            }
            return sb.toString();
        }

        private String escapeCSV(String value) {
            if (value == null) return "";
            return value.replaceAll("\"", "\"\"");
        }
    }

    // Debug method to test parser
    public static void main(String[] args) {
        // Test HTML
        String testHtml = """
            <div class="product-tile">
                <h2 class="product-name">Test Product</h2>
                <div class="product-price">$299.99</div>
                <ul class="feature-list">
                    <li>Feature 1</li>
                    <li>Feature 2</li>
                </ul>
                <div class="product-description">Product description here</div>
            </div>
        """;

        List<Product> products = extractProducts(testHtml, "BOSE");
        for (Product product : products) {
            System.out.println("\nExtracted Product:");
            System.out.println("Name: " + product.getName());
            System.out.println("Price: $" + product.getPrice());
            System.out.println("Features: " + product.getFeatures());
            System.out.println("Description: " + product.getDescription());
        }
    }
}