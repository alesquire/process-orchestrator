@echo off
echo Starting Process UI Module...
echo.
echo The Process Dashboard will be available at: http://localhost:8082
echo.
echo Login credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo Press Ctrl+C to stop the application
echo.
cd process-ui-module
call mvn spring-boot:run
echo.
echo Application stopped. Press Enter to continue...
pause
