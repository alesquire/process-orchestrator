# Process Orchestrator Docker Management Script (PowerShell)

Write-Host "Process Orchestrator Docker Management" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green

function Show-Menu {
    Write-Host ""
    Write-Host "Choose an option:" -ForegroundColor Yellow
    Write-Host "1. Start PostgreSQL and pgAdmin"
    Write-Host "2. Stop all services"
    Write-Host "3. Restart all services"
    Write-Host "4. View logs"
    Write-Host "5. Clean up (remove containers and volumes)"
    Write-Host "6. Show status"
    Write-Host "7. Exit"
    Write-Host ""
}

function Start-Services {
    Write-Host "Starting PostgreSQL and pgAdmin..." -ForegroundColor Blue
    docker-compose up -d
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Services started successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "PostgreSQL is available at: localhost:5432" -ForegroundColor Cyan
        Write-Host "Database: process_orchestrator" -ForegroundColor Cyan
        Write-Host "Username: postgres" -ForegroundColor Cyan
        Write-Host "Password: your-super-secret-and-long-postgres-password" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "pgAdmin is available at: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "Email: admin@processorchestrator.com" -ForegroundColor Cyan
        Write-Host "Password: admin123" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Data is stored in:" -ForegroundColor Cyan
        Write-Host "- PostgreSQL: .\postgres-data\" -ForegroundColor Cyan
        Write-Host "- pgAdmin: .\pgadmin-data\" -ForegroundColor Cyan
    } else {
        Write-Host "Failed to start services. Please check Docker Desktop is running." -ForegroundColor Red
    }
}

function Stop-Services {
    Write-Host "Stopping all services..." -ForegroundColor Blue
    docker-compose down
    Write-Host "Services stopped." -ForegroundColor Green
}

function Restart-Services {
    Write-Host "Restarting all services..." -ForegroundColor Blue
    docker-compose down
    docker-compose up -d
    Write-Host "Services restarted." -ForegroundColor Green
}

function Show-Logs {
    Write-Host "Choose service to view logs:" -ForegroundColor Yellow
    Write-Host "1. PostgreSQL"
    Write-Host "2. pgAdmin"
    Write-Host "3. All services"
    $logChoice = Read-Host "Enter your choice (1-3)"
    
    switch ($logChoice) {
        "1" { docker-compose logs -f postgres }
        "2" { docker-compose logs -f pgadmin }
        "3" { docker-compose logs -f }
        default { Write-Host "Invalid choice." -ForegroundColor Red }
    }
}

function Cleanup-Services {
    Write-Host "WARNING: This will remove all containers and data!" -ForegroundColor Red
    $confirm = Read-Host "Are you sure? (y/N)"
    
    if ($confirm -eq "y" -or $confirm -eq "Y") {
        Write-Host "Cleaning up..." -ForegroundColor Blue
        docker-compose down -v
        docker system prune -f
        Write-Host "Cleanup completed." -ForegroundColor Green
    } else {
        Write-Host "Cleanup cancelled." -ForegroundColor Yellow
    }
}

function Show-Status {
    Write-Host "Checking service status..." -ForegroundColor Blue
    docker-compose ps
    Write-Host ""
    Write-Host "Container health:" -ForegroundColor Blue
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Main menu loop
do {
    Show-Menu
    $choice = Read-Host "Enter your choice (1-7)"
    
    switch ($choice) {
        "1" { Start-Services }
        "2" { Stop-Services }
        "3" { Restart-Services }
        "4" { Show-Logs }
        "5" { Cleanup-Services }
        "6" { Show-Status }
        "7" { 
            Write-Host "Goodbye!" -ForegroundColor Green
            break 
        }
        default { Write-Host "Invalid choice. Please try again." -ForegroundColor Red }
    }
} while ($choice -ne "7")


