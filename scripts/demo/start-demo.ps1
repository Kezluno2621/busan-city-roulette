# 시연용 Spring Boot 프로필을 시작하기 위한 실행 매개변수입니다.
param(
    [int]$Port = 18090,
    [int]$DatabasePort = 33321,
    [string]$Database = "cityroulette_demo",
    [string]$Username = $env:DEMO_DB_USERNAME,
    [switch]$AllowEmptyPassword
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$javaHome = if ($env:DEMO_JAVA_HOME) { $env:DEMO_JAVA_HOME } else { "C:\Program Files\Java\jdk-17" }
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$env:DEMO_DB_HOST = "127.0.0.1"
$env:DEMO_DB_PORT = $DatabasePort.ToString()
$env:DEMO_DB_NAME = $Database
$env:DEMO_DB_USERNAME = $Username
$env:DEMO_SERVER_PORT = $Port.ToString()
$env:DEMO_UPLOAD_DIR = Join-Path $projectRoot ".runtime-demo\uploads"

$occupiedPids = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique)
if ($occupiedPids.Count -gt 0) {
    $owners = @($occupiedPids | ForEach-Object {
        $owner = Get-Process -Id $_ -ErrorAction SilentlyContinue
        if ($null -eq $owner) { "PID $_" } else { "$($owner.ProcessName) (PID $_)" }
    }) -join ", "
    throw "Spring Boot port $Port is already in use by $owners. Stop that process or choose another -Port."
}

if ($AllowEmptyPassword) {
    $env:DEMO_DB_PASSWORD = ""
} elseif ($null -eq $env:DEMO_DB_PASSWORD) {
    $secure = Read-Host "Demo DB password" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:DEMO_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

& (Join-Path $PSScriptRoot "verify-demo.ps1") -Database $Database -Port $DatabasePort `
    -Username $Username -AllowEmptyPassword:$AllowEmptyPassword
if ($LASTEXITCODE -ne 0) { throw "Demo verification failed." }

Write-Host "Starting City Roulette demo at http://127.0.0.1:$Port/"
Write-Host "Open this URL in Chromium after 'Started CityRouletteApplication' appears."
Write-Host "Press Ctrl+C to stop Spring Boot. Saved database rows remain in the demo MySQL data directory."
Push-Location $projectRoot
try {
    # Do not reuse a Gradle daemon that may have been started with different DB
    # environment variables. Demo credentials must come from this invocation.
    & ".\gradlew.bat" --no-daemon bootRun "--args=--spring.profiles.active=demo"
} finally {
    Pop-Location
    $env:DEMO_DB_PASSWORD = $null
}
