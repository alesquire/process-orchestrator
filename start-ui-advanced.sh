#!/bin/bash

# DB Scheduler UI Module Startup Script for Git Bash
# This script provides multiple options for starting the web-based dashboard

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_color $RED "Error: Maven is not installed or not in PATH"
        print_color $YELLOW "Please install Maven and ensure it's available in your PATH"
        return 1
    fi
    return 0
}

# Function to check if the UI module exists
check_ui_module() {
    if [ ! -d "db-scheduler-ui-module" ]; then
        print_color $RED "Error: db-scheduler-ui-module directory not found"
        print_color $YELLOW "Please ensure you're running this script from the project root"
        return 1
    fi
    
    if [ ! -f "db-scheduler-ui-module/pom.xml" ]; then
        print_color $RED "Error: pom.xml not found in db-scheduler-ui-module directory"
        return 1
    fi
    
    return 0
}

# Function to start the UI
start_ui() {
    print_color $GREEN "Starting DB Scheduler UI Module..."
    echo ""
    print_color $CYAN "The UI will be available at: http://localhost:8081/db-scheduler"
    echo ""
    print_color $YELLOW "Press Ctrl+C to stop the application"
    echo ""
    
    # Change to the UI module directory
    cd db-scheduler-ui-module
    
    # Start the Spring Boot application
    print_color $BLUE "Starting Spring Boot application..."
    mvn spring-boot:run
}

# Function to show help
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -c, --clean    Clean and rebuild before starting"
    echo "  -d, --debug    Enable debug logging"
    echo "  -p, --port     Specify custom port (default: 8081)"
    echo ""
    echo "Examples:"
    echo "  $0                    # Start UI with default settings"
    echo "  $0 --clean            # Clean build and start UI"
    echo "  $0 --debug            # Start UI with debug logging"
    echo "  $0 --port 9090        # Start UI on port 9090"
}

# Function to clean and rebuild
clean_build() {
    print_color $YELLOW "Cleaning and rebuilding project..."
    cd db-scheduler-ui-module
    mvn clean install -DskipTests
    cd ..
}

# Function to set custom port
set_port() {
    local port=$1
    print_color $YELLOW "Setting custom port: $port"
    
    # Update application.properties with custom port
    if [ -f "db-scheduler-ui-module/src/main/resources/application.properties" ]; then
        sed -i "s/server.port=.*/server.port=$port/" db-scheduler-ui-module/src/main/resources/application.properties
        print_color $GREEN "Port updated to $port"
    else
        print_color $RED "Error: application.properties not found"
        return 1
    fi
}

# Function to enable debug logging
enable_debug() {
    print_color $YELLOW "Enabling debug logging..."
    
    if [ -f "db-scheduler-ui-module/src/main/resources/application.properties" ]; then
        # Enable debug logging for db-scheduler-ui
        if ! grep -q "logging.level.no.bekk.db-scheduler-ui=DEBUG" db-scheduler-ui-module/src/main/resources/application.properties; then
            echo "logging.level.no.bekk.db-scheduler-ui=DEBUG" >> db-scheduler-ui-module/src/main/resources/application.properties
        fi
        print_color $GREEN "Debug logging enabled"
    else
        print_color $RED "Error: application.properties not found"
        return 1
    fi
}

# Main script logic
main() {
    local clean_build_flag=false
    local debug_flag=false
    local custom_port=""
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--clean)
                clean_build_flag=true
                shift
                ;;
            -d|--debug)
                debug_flag=true
                shift
                ;;
            -p|--port)
                custom_port="$2"
                shift 2
                ;;
            *)
                print_color $RED "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Check prerequisites
    if ! check_maven; then
        exit 1
    fi
    
    if ! check_ui_module; then
        exit 1
    fi
    
    # Apply options
    if [ "$clean_build_flag" = true ]; then
        clean_build
    fi
    
    if [ "$debug_flag" = true ]; then
        enable_debug
    fi
    
    if [ -n "$custom_port" ]; then
        set_port "$custom_port"
    fi
    
    # Start the UI
    start_ui
    
    # Wait for user input before closing
    echo ""
    print_color $CYAN "Application stopped. Press Enter to continue..."
    read
}

# Run main function with all arguments
main "$@"
