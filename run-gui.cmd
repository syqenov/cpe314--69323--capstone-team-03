@echo off
rem ============================================================
rem  Pet Shelter & Adoption Tracker - launcher
rem  Builds the jar on first run, then starts the Swing GUI.
rem  Requirements: JDK 17+ on your PATH (java / javaw).
rem ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on your PATH. Please install JDK 17 or newer.
    exit /b 1
)

if not exist "target\pet-shelter-tracker.jar" (
    echo Building the application for the first time...
    call "mvnw.cmd" -q -DskipTests package
    if errorlevel 1 (
        echo [ERROR] Build failed.
        exit /b 1
    )
)

echo Starting Pet Shelter and Adoption Tracker...
where javaw >nul 2>nul
if errorlevel 1 (
    start "Pet Shelter ^& Adoption Tracker" java -jar "target\pet-shelter-tracker.jar"
) else (
    start "Pet Shelter ^& Adoption Tracker" javaw -jar "target\pet-shelter-tracker.jar"
)
exit /b 0
