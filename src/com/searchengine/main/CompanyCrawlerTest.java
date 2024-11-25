package com.searchengine.main;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.nio.file.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;

public class CompanyCrawlerTest {
    private static final Map<String, String> COMPANY_URLS = new LinkedHashMap<>() {{
        put("BOSE", "https://www.bose.ca/en/c/home-theater");
        put("SONY", "https://www.sony.ca/en/home-theatre-sound-bars/home-theatre-systems");
        put("LG", "https://www.lg.com/ca_en/tv-soundbars/soundbars/?ec_model_status_code=ACTIVE");
        put("SAMSUNG", "https://www.samsung.com/us/televisions-home-theater/home-theater/all-home-theater/");
    }};

    private static final String BASE_DIR = "html_pages";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String CACHE_LOG = "cache_log.txt";

    public static void main(String[] args) {
        System.out.println("Starting Structured HTML Crawler...\n");

        // Create base directory
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            initializeCacheLog();
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
            return;
        }

        WebDriver driver = null;
        try {
            driver = initializeWebDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Process each company
            for (Map.Entry<String, String> company : COMPANY_URLS.entrySet()) {
                String companyName = company.getKey();
                String url = company.getValue();

                processCompany(driver, wait, companyName, url);
            }

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
            System.out.println("\nCrawling completed!");
            printCacheSummary();
        }
    }

    private static WebDriver initializeWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-popup-blocking",
                "--start-maximized"
        );

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        return driver;
    }

    private static void processCompany(WebDriver driver, WebDriverWait wait,
                                       String companyName, String url) {
        System.out.println("\nProcessing " + companyName);
        System.out.println("URL: " + url);

        try {
            // Create company directory
            Path companyDir = Paths.get(BASE_DIR, companyName.toLowerCase());
            Files.createDirectories(companyDir);

            // Fetch and save main page
            System.out.println("Fetching main page...");
            driver.get(url);

            // Wait for page load and scroll
            Thread.sleep(5000);

            // Scroll down to load more products if needed
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(2000);
            }

            // Save main page
            String mainPageName = "main_" + DATE_FORMAT.format(new Date()) + ".html";
            Path mainPagePath = companyDir.resolve(mainPageName);
            Files.writeString(mainPagePath, driver.getPageSource());
            System.out.println("✅ Saved main page: " + mainPageName);

            // Update cache log
            updateCacheLog(companyName, "Main page updated");

            // Get product links
            List<String> productLinks = extractProductLinks(driver, companyName);
            System.out.println("Found " + productLinks.size() + " product links");

            if (productLinks.isEmpty()) {
                System.out.println("⚠️ No product links found for " + companyName);
                System.out.println("Page source preview:");
                System.out.println(driver.getPageSource().substring(0, 500) + "...");
            }

            // Create products directory
            Path productsDir = companyDir.resolve("products");
            Files.createDirectories(productsDir);

            // Process each product
            for (String productUrl : productLinks) {
                processProduct(driver, wait, productsDir, productUrl);
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing " + companyName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processProduct(WebDriver driver, WebDriverWait wait,
                                       Path productsDir, String productUrl) {
        try {
            driver.get(productUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000); // Wait for dynamic content

            // Generate filename from URL
            String filename = generateFilenameFromUrl(productUrl);
            Path productPath = productsDir.resolve(filename);

            // Save product page
            Files.writeString(productPath, driver.getPageSource());
            System.out.println("✅ Saved product: " + filename);

        } catch (Exception e) {
            System.err.println("❌ Error processing product: " + productUrl);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static List<String> extractProductLinks(WebDriver driver, String companyName) {
        List<String> links = new ArrayList<>();
        try {
            // Wait for products to load
            Thread.sleep(5000);

            // Company-specific selectors
            List<WebElement> elements = new ArrayList<>();
            switch (companyName.toUpperCase()) {
                case "BOSE":
                    // Bose uses a grid layout with product cards
                    elements = driver.findElements(By.cssSelector(".product-tile a, .product-grid a"));
                    System.out.println("Found " + elements.size() + " elements for Bose");
                    break;

                case "SONY":
                    // Sony uses a product listing with specific classes
                    elements = driver.findElements(
                            By.cssSelector(".product-item a, .product-list-item a, .product-tile a"));
                    System.out.println("Found " + elements.size() + " elements for Sony");
                    break;

                case "LG":
                    // LG uses a product grid with specific data attributes
                    elements = driver.findElements(
                            By.cssSelector("[data-model-status-code='ACTIVE'] a, .product-card a"));
                    // Sometimes LG loads products dynamically, try clicking "Show More" if present
                    try {
                        WebElement showMore = driver.findElement(By.cssSelector(".show-more-button"));
                        if (showMore.isDisplayed()) {
                            showMore.click();
                            Thread.sleep(2000);
                            elements = driver.findElements(
                                    By.cssSelector("[data-model-status-code='ACTIVE'] a, .product-card a"));
                        }
                    } catch (Exception e) {
                        // Show more button might not exist
                    }
                    System.out.println("Found " + elements.size() + " elements for LG");
                    break;

                case "SAMSUNG":
                    // Samsung uses multiple classes for product links
                    elements = driver.findElements(
                            By.cssSelector(".product-card a, .product-item a, .product-finder-result a"));
                    System.out.println("Found " + elements.size() + " elements for Samsung");
                    break;

                case "JBL":
                    // JBL uses standard product tiles
                    elements = driver.findElements(
                            By.cssSelector(".product-item a, .product-tile a"));
                    System.out.println("Found " + elements.size() + " elements for JBL");
                    break;
            }

            // Debug output
            System.out.println("Extracting links from " + elements.size() + " elements");

            // Extract and validate links
            for (WebElement element : elements) {
                try {
                    String href = element.getAttribute("href");
                    if (href != null) {
                        System.out.println("Found link: " + href);
                        if (isValidProductUrl(href, companyName)) {
                            links.add(href);
                        }
                    }
                } catch (StaleElementReferenceException e) {
                    // Element might have gone stale, continue with next
                    continue;
                }
            }

            // Debug output
            System.out.println("Found " + links.size() + " valid product links for " + companyName);

            // Remove duplicates
            links = new ArrayList<>(new LinkedHashSet<>(links));

        } catch (Exception e) {
            System.err.println("Error extracting links for " + companyName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return links;
    }

    private static boolean isValidProductUrl(String url, String companyName) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Convert URL and company name to lowercase for comparison
        url = url.toLowerCase();
        companyName = companyName.toLowerCase();

        // Basic validation that it's a product URL
        boolean isValid = false;

        switch (companyName) {
            case "bose":
                isValid = url.contains("bose.ca") &&
                        url.contains("/products/") &&
                        !url.contains("/c/") &&
                        !url.contains("/categories/");
                break;

            case "sony":
                isValid = url.contains("sony.ca") &&
                        (url.contains("/product/") || url.contains("/home-theatre/")) &&
                        !url.contains("/category/");
                break;

            case "lg":
                isValid = url.contains("lg.com") &&
                        url.contains("/tv-soundbars/") &&
                        !url.contains("?") &&
                        !url.contains("#");
                break;

            case "samsung":
                isValid = url.contains("samsung.com") &&
                        url.contains("/televisions-home-theater/") &&
                        !url.contains("/all-home-theater/") &&
                        !url.contains("/category/");
                break;

            case "jbl":
                isValid = url.contains("jbl.com") &&
                        url.contains("/products/") &&
                        !url.contains("/categories/");
                break;
        }

        if (isValid) {
            System.out.println("Valid product URL: " + url);
        }

        return isValid;
    }


    private static String generateFilenameFromUrl(String url) {
        // Extract product ID or name from URL
        String filename = url.replaceAll(".*?/([^/]+)/?$", "$1")
                .replaceAll("[^a-zA-Z0-9-_]", "_");
        return filename + "_" + DATE_FORMAT.format(new Date()) + ".html";
    }

    private static void initializeCacheLog() throws IOException {
        Path logPath = Paths.get(BASE_DIR, CACHE_LOG);
        if (!Files.exists(logPath)) {
            Files.writeString(logPath, "Cache Log Created: " + new Date() + "\n");
        }
    }

    private static void updateCacheLog(String companyName, String action) {
        try {
            Path logPath = Paths.get(BASE_DIR, CACHE_LOG);
            String entry = String.format("%s - %s: %s%n",
                    new Date(), companyName, action);
            Files.writeString(logPath, entry, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error updating cache log: " + e.getMessage());
        }
    }

    private static void printCacheSummary() {
        try {
            Path basePath = Paths.get(BASE_DIR);
            System.out.println("\nCache Directory Structure:");
            Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> {
                        try {
                            // Get relative path from base directory
                            Path relativePath = basePath.relativize(path);
                            System.out.printf("- %s (%.2f MB)%n",
                                    relativePath.toString(),
                                    Files.size(path) / (1024.0 * 1024.0));
                        } catch (IOException e) {
                            System.out.println("- " + path.getFileName() + " (size unknown)");
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error printing summary: " + e.getMessage());
        }
    }
}







