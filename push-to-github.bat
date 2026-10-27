@echo off
title Push Java OOPS Project to GitHub
cls
echo =================================================================
echo   PUSHING COMMITS TO GITHUB (https://github.com/Maahi2008)       
echo =================================================================
echo.
cd /d "C:\Users\arora\OneDrive\Documents\GitHub\Java-oops-project"
echo Pushing local branch 'main' to origin...
git push origin main
echo.
if %errorlevel% equ 0 (
    echo [SUCCESS] All files pushed to GitHub successfully!
    echo.
    echo Next step: Open your browser and go to:
    echo https://github.com/Maahi2008/Java-oops-project/settings/pages
    echo.
    echo Under "Build and deployment":
    echo 1. Set Branch to: main
    echo 2. Set Folder to: /docs
    echo 3. Click Save!
    echo.
    echo Your site will be live at:
    echo https://maahi2008.github.io/Java-oops-project/
) else (
    echo [NOTE] If a browser window opened, please click "Authorize" or sign in to GitHub.
)
echo.
pause
