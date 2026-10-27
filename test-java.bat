@echo off
title MUJ Lost and Found - Core Java Verification Suite
cls
echo =================================================================
echo   MANIPAL UNIVERSITY JAIPUR - CAMPUS LOST and FOUND SYSTEM       
echo   Automated Java OOP Verification Test Suite                     
echo =================================================================
echo.
echo [1/2] Compiling Java classes with javac...
javac -cp ".;lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java JavaTestSuite.java Main.java InteractiveConsole.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Please check your JDK setup.
    pause
    exit /b %errorlevel%
)
echo [OK] Compilation successful!
echo.
echo [2/2] Running automated test suite...
echo.
java -cp . JavaTestSuite
echo.
pause
