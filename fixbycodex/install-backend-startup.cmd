@echo off
setlocal
set "SUPPORT_DIR=%~dp0"
set "STARTUP_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "TARGET=%STARTUP_DIR%\RestaurantBookingBackend.cmd"

> "%TARGET%" echo @echo off
>> "%TARGET%" echo start "" /min powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%SUPPORT_DIR%backend-watchdog.ps1"

call "%SUPPORT_DIR%start-backend-watchdog.cmd"
echo Backend watchdog installed in Windows Startup.
pause
