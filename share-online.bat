@echo off
setlocal
title MUJ Lost ^& Found - Online Live Link Generator

echo ================================================================
echo  Manipal University Jaipur - Lost ^& Found Live Web Access
echo ================================================================
echo.

:: 1. Check if ServerLauncher is already running on port 8080
netstat -ano | findstr ":8080" >nul
if %errorlevel% neq 0 (
    echo [1/2] Starting local Java server on port 8080...
    start "MUJ Lost & Found Server" /min cmd /c "run-server.bat"
    timeout /t 3 /nobreak >nul
) else (
    echo [1/2] Local server is already running on port 8080.
)

echo.
echo [2/2] Generating instant secure public HTTPS link...
echo.
echo ----------------------------------------------------------------
echo Look for the https://....pinggy.link URL below:
echo Share that link with anyone on their mobile phone or laptop!
echo (Keep this window open to maintain the connection. Press Ctrl+C to stop)
echo ----------------------------------------------------------------
echo.

ssh -p 443 -o StrictHostKeyChecking=no -R0:localhost:8080 a.pinggy.io

pause
