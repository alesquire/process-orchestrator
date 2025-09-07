@echo off
echo Starting DB Scheduler UI Module...
echo.
echo The UI will be available at: http://localhost:8080/db-scheduler
echo.
echo Press Ctrl+C to stop the application
echo.

cd db-scheduler-ui-module
mvn spring-boot:run

pause
