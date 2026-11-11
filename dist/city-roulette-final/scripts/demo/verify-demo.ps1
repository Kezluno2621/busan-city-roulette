param(
    [string]$Database = "cityroulette_demo",
    [int]$Port = 33321,
    [string]$Username = $env:DEMO_DB_USERNAME,
    [switch]$AllowEmptyPassword
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$javaHome = if ($env:DEMO_JAVA_HOME) { $env:DEMO_JAVA_HOME } else { "C:\Program Files\Java\jdk-17" }
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $java)) { throw "Java 17 was not found. Set DEMO_JAVA_HOME." }
$javaVersion = & $java -version 2>&1 | Select-Object -First 1
if ($javaVersion -notmatch 'version "17\.') { throw "Demo requires Java 17. Found: $javaVersion" }
if ([string]::IsNullOrWhiteSpace($Username)) { throw "Set DEMO_DB_USERNAME before verification." }

$password = $env:DEMO_DB_PASSWORD
if ($null -eq $password -and -not $AllowEmptyPassword) {
    $secure = Read-Host "Demo DB password" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}
if ([string]::IsNullOrEmpty($password) -and -not $AllowEmptyPassword) {
    throw "Empty DB password requires the explicit -AllowEmptyPassword switch."
}

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path -LiteralPath $mysql)) { throw "MySQL 8.0 client was not found." }
$backupPath = Join-Path $projectRoot ".runtime-demo\backup\cityroulette-demo-sanitized.sql"
$expectedBackupHash = "F14E6C7B80858E0A6F86EC05A0191E218BA0EB072CCC8A01EFFCFE424C363A1E"
if (-not (Test-Path -LiteralPath $backupPath)) {
    throw "Demo recovery backup was not found: $backupPath"
}
$actualBackupHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $backupPath).Hash
if ($actualBackupHash -ne $expectedBackupHash) {
    throw "Demo recovery backup SHA-256 mismatch. Do not continue the rehearsal."
}
$previousMysqlPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $password
    $sql = "SELECT COUNT(*) FROM places; SELECT COUNT(*) FROM place_sources; SELECT COUNT(*) FROM bus_stops; SELECT COUNT(*) FROM bus_routes; SELECT COUNT(*) FROM bus_route_stops; SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version='12'; SELECT COUNT(*) FROM members; SELECT COUNT(*) FROM place_favorites; SELECT COUNT(*) FROM saved_courses; SELECT COUNT(*) FROM saved_course_places;"
    $values = @(& $mysql "--host=127.0.0.1" "--port=$Port" "--user=$Username" "--batch" "--skip-column-names" "--database=$Database" "--execute=$sql" 2>$null)
    $queryExitCode = $LASTEXITCODE
    if ($queryExitCode -ne 0 -or $values.Count -ne 10) {
        throw "Cannot verify demo DB at 127.0.0.1:$Port/$Database. Run start-demo-db.ps1 and check the demo credentials."
    }
} finally {
    $env:MYSQL_PWD = $previousMysqlPassword
    $password = $null
}

$expectedExact = @(395, 395, 30, 1, 20, 1, 1)
for ($index = 0; $index -lt $expectedExact.Count; $index++) {
    if ([int]$values[$index] -ne $expectedExact[$index]) {
        throw "Demo DB baseline mismatch at check $($index + 1): expected $($expectedExact[$index]), actual $($values[$index])."
    }
}
$expectedMinimum = @(1, 1, 3)
for ($offset = 0; $offset -lt $expectedMinimum.Count; $offset++) {
    $valueIndex = $expectedExact.Count + $offset
    if ([int]$values[$valueIndex] -lt $expectedMinimum[$offset]) {
        throw "Demo DB presentation data is missing at check $($valueIndex + 1): expected at least $($expectedMinimum[$offset]), actual $($values[$valueIndex])."
    }
}
Write-Host "Java: $javaVersion"
Write-Host "Backup SHA-256 verified: $actualBackupHash"
Write-Host "Demo DB verified: Place=395, PlaceSource=395, BusStop=30, BusRoute=1, BusRouteStop=20, Flyway=V12."
Write-Host "Presentation data verified: Member=$($values[6]), Favorite=$($values[7]), Course=$($values[8]), CoursePlace=$($values[9])."
