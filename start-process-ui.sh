#!/bin/bash
echo "Starting Process UI Module..."
echo ""
echo "The Process Dashboard will be available at: http://localhost:8082"
echo ""
echo "Login credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven and ensure it's available in your PATH"
    exit 1
fi

if [ ! -f "pom.xml" ]; then
    echo "Error: pom.xml not found in project root"
    echo "Please ensure you're running this script from the project root"
    exit 1
fi

echo "Building core module and dependencies..."
mvn clean install -pl process-orchestrator-core -am -DskipTests
echo ""
echo "Starting Spring Boot application..."
mvn spring-boot:run -pl process-ui-module
echo ""
echo "Application stopped. Press Enter to continue..."
read
