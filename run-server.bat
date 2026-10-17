@echo off
title Campus Lost and Found Matching System - Web Server
echo ========================================================
echo   Campus Lost ^& Found Matching System
echo   Launching Web Application on http://localhost:8080/
echo ========================================================
echo.
javac -cp ".;lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java web/*.java ServerLauncher.java
if %ERRORLEVEL% EQU 0 (
    echo [OK] Compilation successful.
    echo Starting server...
    java -cp ".;lib/javax.servlet-api-4.0.1.jar" ServerLauncher
) else (
    echo [ERROR] Compilation failed.
    pause
)
