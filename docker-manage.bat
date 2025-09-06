@echo off
REM Process Orchestrator Docker Management Script

echo Process Orchestrator Docker Management
echo ======================================

:menu
echo.
echo Choose an option:
echo 1. Start PostgreSQL and pgAdmin
echo 2. Stop all services
echo 3. Restart all services
echo 4. View logs
echo 5. Clean up (remove containers and volumes)
echo 6. Show status
echo 7. Exit
echo.
set /p choice="Enter your choice (1-7): "

if "%choice%"=="1" goto start
if "%choice%"=="2" goto stop
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto logs
if "%choice%"=="5" goto cleanup
if "%choice%"=="6" goto status
if "%choice%"=="7" goto exit
echo Invalid choice. Please try again.
goto menu

:start
echo Starting PostgreSQL and pgAdmin...
docker-compose up -d
if %errorlevel% equ 0 (
    echo.
    echo Services started successfully!
    echo.
    echo PostgreSQL is available at: localhost:5432
    echo Database: process_orchestrator
    echo Username: postgres
    echo Password: your-super-secret-and-long-postgres-password
    echo.
    echo pgAdmin is available at: http://localhost:8080
    echo Email: admin@processorchestrator.com
    echo Password: admin123
    echo.
    echo Data is stored in:
    echo - PostgreSQL: .\postgres-data\
    echo - pgAdmin: .\pgadmin-data\
) else (
    echo Failed to start services. Please check Docker Desktop is running.
)
goto menu

:stop
echo Stopping all services...
docker-compose down
echo Services stopped.
goto menu

:restart
echo Restarting all services...
docker-compose down
docker-compose up -d
echo Services restarted.
goto menu

:logs
echo Choose service to view logs:
echo 1. PostgreSQL
echo 2. pgAdmin
echo 3. All services
set /p logchoice="Enter your choice (1-3): "

if "%logchoice%"=="1" (
    docker-compose logs -f postgres
) else if "%logchoice%"=="2" (
    docker-compose logs -f pgadmin
) else if "%logchoice%"=="3" (
    docker-compose logs -f
) else (
    echo Invalid choice.
)
goto menu

:cleanup
echo WARNING: This will remove all containers and data!
set /p confirm="Are you sure? (y/N): "
if /i "%confirm%"=="y" (
    echo Cleaning up...
    docker-compose down -v
    docker system prune -f
    echo Cleanup completed.
) else (
    echo Cleanup cancelled.
)
goto menu

:status
echo Checking service status...
docker-compose ps
echo.
echo Container health:
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
goto menu

:exit
echo Goodbye!
exit /b 0


