#!/bin/bash

# DB Scheduler UI Module Startup Script for Git Bash
# This script starts the web-based dashboard for monitoring and managing db-scheduler tasks

echo "Starting DB Scheduler UI Module..."
echo ""
echo "The UI will be available at: http://localhost:8081/db-scheduler"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

# Change to the UI module directory
cd db-scheduler-ui-module

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven and ensure it's available in your PATH"
    exit 1
fi

# Check if the pom.xml exists
if [ ! -f "pom.xml" ]; then
    echo "Error: pom.xml not found in db-scheduler-ui-module directory"
    echo "Please ensure you're running this script from the project root"
    exit 1
fi

# Start the Spring Boot application
echo "Starting Spring Boot application..."
mvn spring-boot:run

# Wait for user input before closing
echo ""
echo "Application stopped. Press Enter to continue..."
read
