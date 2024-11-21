// File: build.sh
#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Building Search Engine Crawler Test..."

# Clean bin directory
echo "Cleaning previous build..."
rm -rf bin/*
mkdir -p bin

# Create classpath with all dependencies
CLASSPATH="."
for jar in lib/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# Compile
echo "Compiling Java files..."
find src -name "*.java" > sources.txt
javac -cp "$CLASSPATH" -d bin @sources.txt

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Build successful!${NC}"
    echo "Running crawler test..."
    java -cp "bin:$CLASSPATH" com.searchengine.main.SimpleCrawlerTest
else
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

# Clean up sources file
rm sources.txt