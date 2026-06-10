RESTAURANT BOOKING BACKEND RECOVERY

All Codex support files are stored in this fixcode directory.
The Android project remains in the parent directory.

FIRST SETUP ON A NEW COMPUTER

1. Install Node.js and SQL Server.
2. Restore/import the RestaurantDB database.
3. Open restaurant-backend and run npm install.
4. Copy restaurant-backend\.env.example to restaurant-backend\.env and enter
   the SQL Server password for the current computer.
5. Run install-backend-startup.cmd once.
6. Run this health check:

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\check-backend.ps1"

Expected:
API: online
Database: online

FILES

- backend-watchdog.ps1: restarts the backend when port 3001 is unavailable.
- start-backend-watchdog.cmd: starts the watchdog manually.
- install-backend-startup.cmd: installs automatic startup for the current PC.
- remove-backend-startup.cmd: removes automatic startup.
- check-backend.ps1: checks API and SQL Server health.
- logs: local operational logs.

The scripts determine the project path from the fixcode directory, so the
project can be moved or shared without editing absolute paths.

Do not copy C:\Users\asus\.codex into the project. It contains Codex
authentication, active sessions, settings, plugins, and local caches.
