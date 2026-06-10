@echo off
setlocal
set "TARGET=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\RestaurantBookingBackend.cmd"

if exist "%TARGET%" del /q "%TARGET%"
echo Backend watchdog removed from Windows Startup.
pause
