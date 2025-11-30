@echo off
REM Newman Integration Test Runner for Employee Module
REM Prerequisites: npm install -g newman newman-reporter-htmlextra

echo ========================================
echo Employee Module Integration Tests
echo ========================================

REM Check if newman is installed
where newman >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Newman is not installed. Installing...
    npm install -g newman newman-reporter-htmlextra
)

REM Run the tests
newman run employee-module-collection.json ^
    -e employee-module-environment.json ^
    --reporters cli,htmlextra ^
    --reporter-htmlextra-export ./reports/employee-module-report.html ^
    --timeout-request 10000

echo ========================================
echo Test completed. Check reports folder for HTML report.
echo ========================================
pause
