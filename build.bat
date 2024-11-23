@echo off
setlocal EnableDelayedExpansion

:: Color codes for Windows
set "RED=[91m"
set "GREEN=[92m"
set "BLUE=[94m"
set "YELLOW=[93m"
set "CYAN=[96m"
set "PURPLE=[95m"
set "NC=[0m"
set "BOLD=[1m"

:: Function for typing effect using PowerShell
:typeText
PowerShell -Command ^
    "$text = '%~1'; $delay = %~2; ^
    $text.ToCharArray() | ForEach-Object { ^
        Write-Host -NoNewline $_; ^
        Start-Sleep -Milliseconds ($delay * 1000) ^
    }; ^
    Write-Host"
exit /b

:: Function for loading animation
:loadingAnimation
set "duration=%~1"
set "message=%~2"
PowerShell -Command ^
    "$duration = %duration%; $message = '%message%'; ^
    $chars = '⠋','⠙','⠹','⠸','⠼','⠴','⠦','⠧','⠇','⠏'; ^
    $start = Get-Date; ^
    while ((Get-Date).Subtract($start).TotalSeconds -lt $duration) { ^
        foreach ($char in $chars) { ^
            Write-Host -NoNewLine "`r[96m${message} ${char}[0m"; ^
            Start-Sleep -Milliseconds 100 ^
        } ^
    }; ^
    Write-Host -NoNewLine "`r[92m${message} ✓[0m`n""
exit /b

:: Function for progress bar
:progressBar
set "duration=%~1"
set "message=%~2"
PowerShell -Command ^
    "$duration = %duration%; $message = '%message%'; ^
    Write-Host -NoNewline $message' '; ^
    $width = 50; ^
    for ($i = 0; $i -lt $width; $i++) { ^
        Write-Host -NoNewline '▰'; ^
        Start-Sleep -Milliseconds ($duration * 1000 / $width) ^
    }; ^
    Write-Host ' [92mDone![0m'"
exit /b

:: Clear screen and show initial animation
cls
timeout /t 1 /nobreak > nul

:: Show banner
echo [93m   _____                     _      _____[0m
echo [93m  / ____|                   ^| ^|    ^|  ___^|[0m
echo [93m ^| (___   ___  __ _ _ __ ___^| ^|__  ^| ^|__   _ __   __ _ (I) _ __   ___[0m
echo [93m  \___ \ / _ \/ _' ^| '__/ __^| '_ \ ^|  __^| ^| '_ \ / _' ^|^| ^|^| '_ \ / _  \[0m
echo [93m  ____) ^|  __/ (_^| ^| ^| ^| (__^| ^| ^| ^| ^|____^|^| ^| ^| ^| (_^| ^|^| ^|^| ^| ^| ^|^|  __/[0m
echo [93m ^|_____/ \___^|\__,_^|_^|  \___^|_^| ^|_^|______^|^|_^| ^|_^|\__, ^|^| ^|^|_^| ^|_^| \___^|[0m
echo [93m                                                  __/ ^|[0m
echo [93m                                                 ^|___/[0m
echo.
echo [1m[95mSearch Engine Builder v1.0[0m
echo.
timeout /t 1 /nobreak > nul

:: Initialize build environment
echo [1m[94m[Phase 1/5][0m [96mInitializing Build Environment[0m
timeout /t 1 /nobreak > nul

:: Create directory structure
call :loadingAnimation 2 "Creating directory structure"
if not exist build\classes mkdir build\classes
if not exist lib mkdir lib
if not exist crawler_cache mkdir crawler_cache
if not exist crawled_data mkdir crawled_data
if not exist product_data mkdir product_data
if not exist logs mkdir logs

echo.
echo [1m[94m[Phase 2/5][0m [96mResolving Dependencies[0m
timeout /t 1 /nobreak > nul

:: Download dependencies with progress bars
if not exist "lib\gson-2.8.9.jar" (
    call :typeText "▶ Downloading Gson Library..." 0.03
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar' -OutFile 'lib\gson-2.8.9.jar'"
    echo [92m✓ Gson downloaded successfully[0m
)

if not exist "lib\selenium-java-4.8.1.jar" (
    call :typeText "▶ Downloading Selenium Framework..." 0.03
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/seleniumhq/selenium/selenium-java/4.8.1/selenium-java-4.8.1.jar' -OutFile 'lib\selenium-java-4.8.1.jar'"
    echo [92m✓ Selenium downloaded successfully[0m
)

if not exist "lib\jsoup-1.15.4.jar" (
    call :typeText "▶ Downloading Jsoup Library..." 0.03
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/jsoup/jsoup/1.15.4/jsoup-1.15.4.jar' -OutFile 'lib\jsoup-1.15.4.jar'"
    echo [92m✓ Jsoup downloaded successfully[0m
)

:: Set classpath
set "CLASSPATH=.\lib\*;.\build\classes"

echo.
echo [1m[94m[Phase 3/5][0m [96mCompiling Source Files[0m
timeout /t 1 /nobreak > nul

:: Clean build directory
call :loadingAnimation 2 "Cleaning build directory"
if exist build\classes\* del /q /s build\classes\*

:: Find and compile source files
call :typeText "▶ Scanning for source files..." 0.03
timeout /t 1 /nobreak > nul
dir /s /b src\*.java > sources.txt
for /f %%i in ('type sources.txt ^| find /c /v ""') do set count=%%i
echo [92m✓ Found !count! source files[0m

call :typeText "▶ Compiling Java sources..." 0.03
call :loadingAnimation 3 "Optimizing and compiling"
javac -cp "%CLASSPATH%" -d build\classes @sources.txt
del sources.txt
echo [92m✓ Compilation successful[0m

echo.
echo [1m[94m[Phase 4/5][0m [96mBuilding JAR Package[0m
timeout /t 1 /nobreak > nul

:: Create manifest
call :typeText "▶ Generating manifest..." 0.03
(
    echo Manifest-Version: 1.0
    echo Main-Class: com.searchengine.main.SearchEngineApplication
    echo Class-Path: lib/gson-2.8.9.jar lib/selenium-java-4.8.1.jar lib/jsoup-1.15.4.jar
) > manifest.txt
timeout /t 1 /nobreak > nul
echo [92m✓ Manifest created[0m

:: Create JAR
call :typeText "▶ Packaging JAR file..." 0.03
call :progressBar 3 "Building JAR"
jar cfm build\SearchEngine.jar manifest.txt -C build\classes .
del manifest.txt

echo.
echo [1m[94m[Phase 5/5][0m [96mFinalizing Build[0m
timeout /t 1 /nobreak > nul

:: Create run script
call :loadingAnimation 2 "Creating launch script"
(
    echo @echo off
    echo java -cp "lib\*;build\SearchEngine.jar" com.searchengine.main.SearchEngineApplication
    echo pause
) > run.bat

:: Final animation
echo.
echo [1m[92mBuild Completed Successfully![0m
echo.
timeout /t 1 /nobreak > nul

:: Show build summary with typing effect
echo [96mBuild Summary:[0m
call :typeText "  [92m✓[0m Source files compiled" 0.03
call :typeText "  [92m✓[0m Dependencies resolved" 0.03
call :typeText "  [92m✓[0m JAR package created" 0.03
call :typeText "  [92m✓[0m Launch script generated" 0.03
echo.
call :typeText "[93mTo launch the application, run: [1mrun.bat[0m" 0.03
echo.

endlocal
pause