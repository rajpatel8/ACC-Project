#!/bin/bash

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Function for typing effect
type_text() {
    text="$1"
    delay=${2:-0.05}
    for ((i=0; i<${#text}; i++)); do
        echo -n "${text:$i:1}"
        sleep $delay
    done
    echo
}

# Function for loading animation
loading_animation() {
    local duration=$1
    local text=$2
    local chars="⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
    local end=$((SECONDS + duration))

    while [ $SECONDS -lt $end ]; do
        for (( i=0; i<${#chars}; i++ )); do
            echo -en "\r${CYAN}${text} ${chars:$i:1} ${NC}"
            sleep 0.1
        done
    done
    echo -en "\r${GREEN}${text} ✓${NC}\n"
}

# Function for progress bar
progress_bar() {
    local duration=$1
    local text=$2
    local width=50
    local progress=0

    echo -n "${text} "
    while [ $progress -lt $width ]; do
        echo -n "▰"
        sleep $(bc -l <<< "$duration/$width")
        progress=$((progress + 1))
    done
    echo -e " ${GREEN}Done!${NC}"
}

# Clear screen and show initial animation
clear
echo -en "${YELLOW}"
sleep 0.5

# Show banner character by character
cat << "EOF" | while IFS= read -r line; do
   _____                     _      _____                _
  / ____|                   | |    |  ___|             (_)
 | (___   ___  __ _ _ __ ___| |__  | |__   _ __   __ _ _ _ __   ___
  \___ \ / _ \/ _' | '__/ __| '_ \ |  __| | '_ \ / _' | | '_ \ / _ \
  ____) |  __/ (_| | | | (__| | | | |____|| | | | (_| | | | | |  __/
 |_____/ \___|\__,_|_|  \___|_| |_|______||_| |_|\__, |_|_| |_|\___|
                                                  __/ |
                                                 |___/
EOF
    echo -en "${YELLOW}$line${NC}\n"
    sleep 0.1
done

echo -e "\n${BOLD}${PURPLE}Product Search & Analytics System v1.0${NC}"
echo -e "${BOLD}${CYAN}Developed by Team SearchMasters${NC}\n"

# Show team info
echo -e "\n${BOLD}${BLUE}Development Credits:${NC}"

# RAJ's contributions
echo -e "\n${CYAN}▶ RAJKUMAR${NC}"
type_text "  ✓ Web Crawler" 0.02
type_text "  ✓ Caching System" 0.02
type_text "  ✓ Search Filters" 0.02

# VANSH's contributions
echo -e "\n${CYAN}▶ VANSH${NC}"
type_text "  ✓ HTML Parser" 0.02
type_text "  ✓ Data Validation System" 0.02
type_text "  ✓ Spell Checking Engine" 0.02

# Team contributions
echo -e "\n${CYAN}▶ Teammate-3${NC}"
type_text "  ✓ Word Completion System" 0.02
type_text "  ✓ Frequency Analysis" 0.02
type_text "  ✓ Search Frequency Tracking" 0.02

echo -e "\n${CYAN}▶ Teammate-4${NC}"
type_text "  ✓ Page Ranking Algorithm" 0.02
type_text "  ✓ Inverted Indexing" 0.02
type_text "  ✓ Pattern Recognition" 0.02

echo -e "\n${CYAN}▶ Teammate-5${NC}"
type_text "  ✓ History Management" 0.02
type_text "  ✓ Statistical Analysis" 0.02
type_text "  ✓ Popular Products Tracking" 0.02
sleep 1

# Initialize build environment
echo -e "\n${BOLD}${BLUE}[Phase 1/5]${NC} ${CYAN}Initializing Build Environment${NC}"
sleep 0.5

# Create directory structure
loading_animation 2 "Creating directory structure"
mkdir -p build/classes lib crawler_cache crawled_data product_data logs

echo -e "\n${BOLD}${BLUE}[Phase 2/5]${NC} ${CYAN}Resolving Dependencies${NC}"
sleep 0.5

# Download dependencies with progress bars
if [ ! -f "lib/gson-2.8.9.jar" ]; then
    type_text "▶ Downloading Gson Library..." 0.03
    curl -L "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar" -o lib/gson-2.8.9.jar --progress-bar
    echo -e "${GREEN}✓ Gson downloaded successfully${NC}"
fi

if [ ! -f "lib/selenium-java-4.8.1.jar" ]; then
    type_text "▶ Downloading Selenium Framework..." 0.03
    curl -L "https://repo1.maven.org/maven2/org/seleniumhq/selenium/selenium-java/4.8.1/selenium-java-4.8.1.jar" -o lib/selenium-java-4.8.1.jar --progress-bar
    echo -e "${GREEN}✓ Selenium downloaded successfully${NC}"
fi

if [ ! -f "lib/jsoup-1.15.4.jar" ]; then
    type_text "▶ Downloading Jsoup Library..." 0.03
    curl -L "https://repo1.maven.org/maven2/org/jsoup/jsoup/1.15.4/jsoup-1.15.4.jar" -o lib/jsoup-1.15.4.jar --progress-bar
    echo -e "${GREEN}✓ Jsoup downloaded successfully${NC}"
fi

# Set classpath
CLASSPATH="./lib/*:./build/classes"

echo -e "\n${BOLD}${BLUE}[Phase 3/5]${NC} ${CYAN}Compiling Source Files${NC}"
sleep 0.5

# Clean build directory
loading_animation 2 "Cleaning build directory"
rm -rf build/classes/*

# Find and compile source files
type_text "▶ Scanning for source files..." 0.03
sleep 1
find src -name "*.java" > sources.txt
echo -e "${GREEN}✓ Found $(wc -l < sources.txt) source files${NC}"

type_text "▶ Compiling Java sources..." 0.03
loading_animation 3 "Optimizing and compiling"
javac -cp "$CLASSPATH" -d build/classes @sources.txt
rm sources.txt
echo -e "${GREEN}✓ Compilation successful${NC}"

echo -e "\n${BOLD}${BLUE}[Phase 4/5]${NC} ${CYAN}Building JAR Package${NC}"
sleep 0.5

# Create manifest
type_text "▶ Generating manifest..." 0.03
{
    echo "Manifest-Version: 1.0"
    echo "Main-Class: com.searchengine.main.SearchEngineApplication"
    echo "Class-Path: lib/gson-2.8.9.jar lib/selenium-java-4.8.1.jar lib/jsoup-1.15.4.jar"
} > manifest.txt
sleep 1
echo -e "${GREEN}✓ Manifest created${NC}"

# Create JAR
type_text "▶ Packaging JAR file..." 0.03
progress_bar 3 "Building JAR"
jar cfm build/SearchEngine.jar manifest.txt -C build/classes .
rm manifest.txt
chmod +x build/SearchEngine.jar

echo -e "\n${BOLD}${BLUE}[Phase 5/5]${NC} ${CYAN}Finalizing Build${NC}"
sleep 0.5

# Create run script
loading_animation 2 "Creating launch script"
cat > run.sh << 'EOF'
#!/bin/bash
java -cp "lib/*:build/SearchEngine.jar" com.searchengine.main.SearchEngineApplication
EOF
chmod +x run.sh

# Final animation
echo -e "\n${BOLD}${GREEN}Build Completed Successfully!${NC}\n"
sleep 0.5

# Show build summary with typing effect
echo -e "${CYAN}Build Summary:${NC}"
type_text "  ${GREEN}✓${NC} Source files compiled" 0.03
type_text "  ${GREEN}✓${NC} Dependencies resolved" 0.03
type_text "  ${GREEN}✓${NC} JAR package created" 0.03
type_text "  ${GREEN}✓${NC} Launch script generated" 0.03
echo
type_text "${YELLOW}To launch the application, run: ${BOLD}./run.sh${NC}" 0.03
echo