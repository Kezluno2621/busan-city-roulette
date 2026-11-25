# 시연에 사용한 MySQL 프로세스에 종료를 요청할 포트입니다.
param([int]$Port = 33321)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pidFile = Join-Path $projectRoot ".runtime-demo\mysql.pid"
$demoPid = if (Test-Path -LiteralPath $pidFile) { [int](Get-Content -LiteralPath $pidFile) } else { 0 }
$process = if ($demoPid -gt 0) { Get-Process -Id $demoPid -ErrorAction SilentlyContinue } else { $null }
if ($null -eq $process) {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 1) { throw "More than one listener was found on demo port $Port." }
    if ($listeners.Count -eq 1) {
        $demoPid = $listeners[0].OwningProcess
        $process = Get-Process -Id $demoPid -ErrorAction SilentlyContinue
    }
    if ($null -eq $process) {
        Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
        Write-Host "Demo MySQL is already stopped."
        exit 0
    }
}
if ($process.ProcessName -ne "mysqld") {
    throw "PID $demoPid is not mysqld; refusing to stop it."
}
Stop-Process -Id $demoPid
Wait-Process -Id $demoPid -Timeout 15 -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
Write-Host "Demo MySQL stopped (PID $demoPid)."
