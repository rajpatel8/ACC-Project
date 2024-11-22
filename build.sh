#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Building Search Engine Crawler Test...${NC}"

# Clean bin directory
echo "Cleaning previous build..."
rm -rf bin/*
mkdir -p bin

# Check for required JARs
required_jars=("selenium-java" "gson")
missing_jars=()

for jar in "${required_jars[@]}"; do
    if ! ls lib/*$jar*.jar 1> /dev/null 2>&1; then
        missing_jars+=($jar)
    fi
done

if [ ${#missing_jars[@]} -ne 0 ]; then
    echo -e "${RED}Error: Missing required JARs: ${missing_jars[*]}${NC}"
    echo "Please ensure all required JARs are in the lib directory"
    exit 1
fi

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