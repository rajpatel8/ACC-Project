@echo off
setlocal EnableDelayedExpansion

:: Create directories
mkdir build\classes lib crawler_cache crawled_data product_data logs 2>nul

:: Download dependencies if they don't exist
if not exist "lib\gson-2.8.9.jar" (
    echo Downloading Gson...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar' -OutFile 'lib\gson-2.8.9.jar'"
)

if not exist "lib\selenium-java-4.8.1.jar" (
    echo Downloading Selenium...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/seleniumhq/selenium/selenium-java/4.8.1/selenium-java-4.8.1.jar' -OutFile 'lib\selenium-java-4.8.1.jar'"
)

if not exist "lib\jsoup-1.15.4.jar" (
    echo Downloading Jsoup...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/jsoup/jsoup/1.15.4/jsoup-1.15.4.jar' -OutFile 'lib\jsoup-1.15.4.jar'"
)

:: Set classpath
set "CLASSPATH=.\lib\*;.\build\classes"

:: Clean build directory
del /q /s build\classes\* 2>nul

:: Compile source files
dir /s /b src\*.java > sources.txt
javac -cp "%CLASSPATH%" -d build\classes @sources.txt
del sources.txt

:: Create manifest
(
    echo Manifest-Version: 1.0
    echo Main-Class: com.searchengine.main.SearchEngineApplication
    echo Class-Path: lib/gson-2.8.9.jar lib/selenium-java-4.8.1.jar lib/jsoup-1.15.4.jar
) > manifest.txt

:: Create JAR
jar cfm build\SearchEngine.jar manifest.txt -C build\classes .
del manifest.txt

:: Create run script
(
    echo @echo off
    echo java -cp "lib\*;build\SearchEngine.jar" com.searchengine.main.SearchEngineApplication
    echo pause
) > run.bat

echo Build complete. Run 'run.bat' to start the application.
pause