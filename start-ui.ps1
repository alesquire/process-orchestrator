# DB Scheduler UI Module Startup Script for PowerShell
# This script starts the web-based dashboard for monitoring and managing db-scheduler tasks

Write-Host "Starting DB Scheduler UI Module..." -ForegroundColor Green
Write-Host ""
Write-Host "The UI will be available at: http://localhost:8081/db-scheduler" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Yellow
Write-Host ""

# Change to the UI module directory
Set-Location db-scheduler-ui-module

# Check if Maven is available
try {
    $mvnVersion = mvn -version 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Maven not found"
    }
} catch {
    Write-Host "Error: Maven is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Maven and ensure it's available in your PATH" -ForegroundColor Yellow
    exit 1
}

# Check if the pom.xml exists
if (-not (Test-Path "pom.xml")) {
    Write-Host "Error: pom.xml not found in db-scheduler-ui-module directory" -ForegroundColor Red
    Write-Host "Please ensure you're running this script from the project root" -ForegroundColor Yellow
    exit 1
}

# Start the Spring Boot application
Write-Host "Starting Spring Boot application..." -ForegroundColor Blue
mvn spring-boot:run

# Wait for user input before closing
Write-Host ""
Write-Host "Application stopped. Press Enter to continue..." -ForegroundColor Cyan
Read-Host