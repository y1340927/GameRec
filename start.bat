@echo off
chcp 65001 >nul
title GameRec System

cd /d "%~dp0"

echo.
echo ===========================================================
echo   GameRec - Smart Game Recommend System (Portable Edition)
echo ===========================================================
echo.
echo Starting services with PowerShell launcher...
echo.
echo   .\start.ps1 -Setup      - Check environment only
echo   .\start.ps1 -Quick      - Skip rebuild, start from existing
echo   .\start.ps1 -ExportDB   - Export database to SQL dump
echo   .\start.ps1 -InitDB     - Initialize database from dump
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Launch failed (code: %errorlevel%)
    echo Check the PowerShell output above for details.
    echo.
)

pause
