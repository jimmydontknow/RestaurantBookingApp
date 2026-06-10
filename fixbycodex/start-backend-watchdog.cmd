@echo off
set "SUPPORT_DIR=%~dp0"
start "" /min powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%SUPPORT_DIR%backend-watchdog.ps1"
