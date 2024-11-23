# Java Search Engine Project

A sophisticated search engine implementation in Java that crawls, indexes, and searches product information with a focus on audio equipment retailers. The project includes web crawling, text analysis, pattern matching, and a GUI interface.

## Features

### Web Crawling
- Configurable web crawler with support for multiple threads
- Selenium-based page rendering for JavaScript-heavy sites
- Automatic retry mechanism for failed requests
- Respects robots.txt and site-specific crawling rules
- Configurable crawl depth and page limits

### Content Processing
- HTML parsing with support for structured product data
- Pattern matching for product specifications
- Price and feature extraction
- Image URL collection
- Metadata extraction

### Search Capabilities
- Inverted index for fast text search
- TF-IDF based ranking system
- Spell checking and word completion
- Pattern-based product matching
- Category-based boosting

### Data Analysis
- Word frequency analysis
- Product pattern detection
- Search term tracking
- Performance metrics collection
- Data validation and cleaning

### Caching System
- File-based page caching
- Configurable cache expiration
- Memory and disk cache coordination
- Cache size management

### User Interface
- Swing-based GUI with tabbed interface
- Real-time search suggestions
- Crawling progress monitoring
- Analysis visualization
- Export functionality

## Prerequisites

- Java 17 or higher
- Chrome/Chromium browser
- ChromeDriver matching your Chrome version
- Build scripts (build.sh for Unix/Linux/Mac, build.bat for Windows)

## Dependencies

- Selenium WebDriver for web crawling
- Jsoup for HTML parsing
- Google Gson for JSON processing
- JUnit for testing
- React and Tailwind CSS for UI components

## Project Structure

```
src/
├── com/
│   └── searchengine/
│       ├── core/
│       │   ├── cache/       # Caching system
│       │   ├── crawler/     # Web crawler
│       │   ├── completion/  # Word completion
│       │   ├── frequency/   # Word frequency analysis
│       │   ├── indexing/    # Search indexing
│       │   ├── parser/      # HTML parsing
│       │   ├── patterns/    # Pattern matching
│       │   ├── ranking/     # Search result ranking
│       │   ├── search/      # Search implementation
│       │   ├── spell/       # Spell checking
│       │   └── validation/  # Data validation
│       ├── model/           # Data models
│       └── ui/              # User interface
```

## Configuration

The application can be configured through `CrawlerConfig.java` and `CacheConfig.java`. Key configuration options include:

- Maximum crawl depth
- Number of crawler threads
- Page load timeout
- Cache directory location
- Cache expiration time
- Maximum cache size

## Usage

1. **Starting the Application**
   ```bash
   java -jar searchengine.jar
   ```

2. **Crawling Products**
   - Select the "Crawler" tab
   - Choose a website from the dropdown
   - Click "Start Crawling"

3. **Searching Products**
   - Enter search terms in the search bar
   - Use suggested completions
   - View ranked results

4. **Analyzing Data**
   - Navigate to the "Analysis" tab
   - View word frequencies and patterns
   - Export analysis results

## Development

To build the project:

On Unix/Linux/Mac:
```bash
./build.sh
```

On Windows:
```batch
build.bat
```

To run tests:

On Unix/Linux/Mac:
```bash
./run_tests.sh
```

On Windows:
```batch
run_tests.bat
```

## Performance Considerations

- The crawler uses a thread pool for parallel processing
- Caching reduces load on target websites
- Memory usage is managed through configurable limits
- Search operations are optimized using inverted indexing

## Error Handling

- Automatic retry for failed crawl attempts
- Graceful degradation for JavaScript-disabled sites
- Validation for parsed product data
- Comprehensive error logging

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Authors

Created by [Rajkumar], [Vansh]

## Acknowledgments

- Inspired by modern search engine architectures
- Uses design patterns from "Clean Code" by Robert C. Martin
- Built with best practices from "Effective Java" by Joshua Bloch
