@echo off
REM PostgreSQL Connection Test Script

echo Testing PostgreSQL connection...
echo.

REM Test 1: Using psql if available
echo Test 1: Direct psql connection
psql -h localhost -p 5432 -U postgres -d process_orchestrator -c "SELECT version();" 2>nul
if %errorlevel% equ 0 (
    echo SUCCESS: Direct psql connection works!
) else (
    echo FAILED: Direct psql connection failed
    echo This might be because psql is not installed on Windows
)
echo.

REM Test 2: Using telnet to check if port is open
echo Test 2: Checking if port 5432 is accessible
echo quit | telnet localhost 5432 2>nul | find "Connected"
if %errorlevel% equ 0 (
    echo SUCCESS: Port 5432 is accessible
) else (
    echo FAILED: Port 5432 is not accessible
    echo This might be a firewall or Docker networking issue
)
echo.

REM Test 3: Using Docker exec (should always work)
echo Test 3: Connection via Docker exec
docker exec process_orchestrator_postgres psql -U postgres -d process_orchestrator -c "SELECT 'Connection test successful' as result;"
if %errorlevel% equ 0 (
    echo SUCCESS: Docker exec connection works!
) else (
    echo FAILED: Docker exec connection failed
)
echo.

echo Connection test completed.
echo.
echo If direct connection failed but Docker exec works, the issue is likely:
echo 1. PostgreSQL client tools not installed on Windows
echo 2. Firewall blocking the connection
echo 3. Docker networking configuration
echo.
echo For pgAdmin configuration:
echo - Host: localhost (or postgres)
echo - Port: 5432
echo - Database: process_orchestrator
echo - Username: postgres
echo - Password: your-super-secret-and-long-postgres-password
pause


