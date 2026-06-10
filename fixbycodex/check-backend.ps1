$ErrorActionPreference = "Stop"

try {
    $health = Invoke-RestMethod -Uri "http://localhost:3001/api/health" -TimeoutSec 10
    Write-Host "API: $($health.api)"
    Write-Host "Database: $($health.database)"
    Write-Host "Uptime: $($health.uptimeSeconds) giay"
    exit 0
}
catch {
    Write-Host "Backend chua san sang: $($_.Exception.Message)"
    exit 1
}
