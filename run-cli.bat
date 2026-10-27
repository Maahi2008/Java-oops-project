@echo off
title MUJ Lost and Found - Interactive Java Console
cls
echo =================================================================
echo   MANIPAL UNIVERSITY JAIPUR - CAMPUS LOST and FOUND SYSTEM       
echo   Interactive OOP Console Test Driver                            
echo =================================================================
echo.
echo Compiling latest classes...
javac -cp ".;lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java JavaTestSuite.java Main.java InteractiveConsole.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %errorlevel%
)
echo.
java -cp . InteractiveConsole
pause
