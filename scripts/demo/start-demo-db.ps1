# 준비된 시연용 MySQL 데이터 디렉터리와 지정 포트를 사용합니다.
param(
    [int]$Port = 33321,
    [string]$DataDirectory = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($DataDirectory)) {
    $DataDirectory = Join-Path $projectRoot ".runtime-demo\mysql-data"
}
$resolvedData = (Resolve-Path -LiteralPath $DataDirectory).Path
$expectedRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot ".runtime-demo"))
if (-not $resolvedData.StartsWith($expectedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Demo MySQL data directory must stay under $expectedRoot"
}
if (-not (Test-Path -LiteralPath (Join-Path $resolvedData "auto.cnf"))) {
    throw "Prepared demo MySQL data was not found: $resolvedData"
}

$mysqlServer = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
if (-not (Test-Path -LiteralPath $mysqlServer)) { throw "MySQL 8.0 server was not found." }
$runtimeRoot = Join-Path $projectRoot ".runtime-demo"
$pidFile = Join-Path $runtimeRoot "mysql.pid"
if (Test-Path -LiteralPath $pidFile) {
    $oldPid = [int](Get-Content -LiteralPath $pidFile)
    if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
        throw "Demo MySQL is already running (PID $oldPid)."
    }
    Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
}

$occupiedPids = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique)
if ($occupiedPids.Count -gt 0) {
    $owners = @($occupiedPids | ForEach-Object {
        $owner = Get-Process -Id $_ -ErrorAction SilentlyContinue
        if ($null -eq $owner) { "PID $_" } else { "$($owner.ProcessName) (PID $_)" }
    }) -join ", "
    throw "Demo DB port $Port is already in use by $owners. Stop that process or choose another -Port."
}

$process = Start-Process -FilePath $mysqlServer `
    -ArgumentList @("--datadir=$resolvedData", "--port=$Port", "--bind-address=127.0.0.1", "--mysqlx=OFF", "--console") `
    -RedirectStandardOutput (Join-Path $runtimeRoot "mysql.out.log") `
    -RedirectStandardError (Join-Path $runtimeRoot "mysql.err.log") `
    -WindowStyle Hidden -PassThru

$listener = $null
for ($attempt = 0; $attempt -lt 40 -and $null -eq $listener; $attempt++) {
    Start-Sleep -Milliseconds 250
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 1) {
        throw "More than one listener was found on demo port $Port."
    }
    if ($listeners.Count -eq 1) { $listener = $listeners[0] }
}
if ($null -eq $listener) {
    if (Get-Process -Id $process.Id -ErrorAction SilentlyContinue) { Stop-Process -Id $process.Id }
    throw "Demo MySQL did not listen on 127.0.0.1:$Port. Check .runtime-demo/mysql.err.log."
}
$serverProcess = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
if ($serverProcess.ProcessName -ne "mysqld") {
    throw "Port $Port is owned by $($serverProcess.ProcessName), not mysqld."
}
Set-Content -LiteralPath $pidFile -Value $serverProcess.Id
Write-Host "Demo MySQL starting on 127.0.0.1:$Port (PID $($serverProcess.Id))."
Write-Host "Run verify-demo.ps1 before starting Spring Boot."
