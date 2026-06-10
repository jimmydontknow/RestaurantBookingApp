$ErrorActionPreference = "SilentlyContinue"

$supportDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $supportDir
$backendDir = Join-Path $projectDir "restaurant-backend"
$logDir = Join-Path $supportDir "logs"
$stdoutLog = Join-Path $logDir "backend-output.log"
$stderrLog = Join-Path $logDir "backend-error.log"
$watchdogLog = Join-Path $logDir "watchdog.log"

$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
$nodeExe = if ($nodeCommand) { $nodeCommand.Source } else { $null }

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

if (-not $nodeExe -or -not (Test-Path -LiteralPath $nodeExe)) {
    Add-Content -LiteralPath $watchdogLog -Encoding UTF8 -Value (
        "$(Get-Date -Format s) node.exe was not found in PATH"
    )
    exit 1
}

if (-not (Test-Path -LiteralPath (Join-Path $backendDir "server.js"))) {
    Add-Content -LiteralPath $watchdogLog -Encoding UTF8 -Value (
        "$(Get-Date -Format s) server.js was not found at $backendDir"
    )
    exit 1
}

$mutex = New-Object System.Threading.Mutex(
    $false,
    "Local\RestaurantBookingBackendWatchdog"
)

if (-not $mutex.WaitOne(0, $false)) {
    exit 0
}

try {
    Add-Content -LiteralPath $watchdogLog -Encoding UTF8 -Value (
        "$(Get-Date -Format s) watchdog started"
    )

    while ($true) {
        $listener = Get-NetTCPConnection -LocalPort 3001 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1

        if (-not $listener) {
            Add-Content -LiteralPath $watchdogLog -Encoding UTF8 -Value (
                "$(Get-Date -Format s) backend missing; starting node"
            )

            Start-Process `
                -FilePath $nodeExe `
                -ArgumentList "server.js" `
                -WorkingDirectory $backendDir `
                -WindowStyle Hidden `
                -RedirectStandardOutput $stdoutLog `
                -RedirectStandardError $stderrLog
        }

        Start-Sleep -Seconds 5
    }
}
finally {
    $mutex.ReleaseMutex()
    $mutex.Dispose()
}
