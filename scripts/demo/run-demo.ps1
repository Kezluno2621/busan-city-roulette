# 시연 DB 시작·검증·앱 실행을 순서대로 연결합니다.
param(
    [int]$Port = 18090,
    [int]$DatabasePort = 33321,
    [string]$Database = "cityroulette_demo",
    [string]$Username = $env:DEMO_DB_USERNAME,
    [switch]$AllowEmptyPassword
)

$ErrorActionPreference = "Stop"
$demoScripts = $PSScriptRoot

try {
    & (Join-Path $demoScripts "start-demo-db.ps1") -Port $DatabasePort
    & (Join-Path $demoScripts "verify-demo.ps1") -Database $Database -Port $DatabasePort `
        -Username $Username -AllowEmptyPassword:$AllowEmptyPassword
    & (Join-Path $demoScripts "start-demo.ps1") -Port $Port -DatabasePort $DatabasePort `
        -Database $Database -Username $Username -AllowEmptyPassword:$AllowEmptyPassword
} finally {
    & (Join-Path $demoScripts "stop-demo-db.ps1") -Port $DatabasePort
}
