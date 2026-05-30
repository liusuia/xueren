# Run as Administrator:
#   powershell -ExecutionPolicy Bypass -File d:\trae\xueren\scripts\setup-mysql.ps1

$ErrorActionPreference = "Stop"

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 9.2\bin"
$mysqld   = Join-Path $mysqlBin "mysqld.exe"
$mysql    = Join-Path $mysqlBin "mysql.exe"
$dataDir  = "C:\ProgramData\MySQL\MySQL Server 9.2\Data"
$configDir = "C:\ProgramData\MySQL\MySQL Server 9.2"
$configFile = Join-Path $configDir "my.ini"
$serviceName = "MySQL92"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootPassword = "xueren123"

$current = New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent()
)
if (-not $current.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "[ERROR] Run PowerShell as Administrator." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $mysqld)) {
    Write-Host "[ERROR] MySQL not found: $mysqld" -ForegroundColor Red
    exit 1
}

Write-Host "[1/5] Create directories..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
New-Item -ItemType Directory -Force -Path $configDir | Out-Null

Write-Host "[2/5] Copy config..." -ForegroundColor Cyan
Copy-Item -Force (Join-Path $scriptDir "my.ini") $configFile

$svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if ($svc) {
    Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
}

if (-not (Test-Path (Join-Path $dataDir "mysql"))) {
    Write-Host "[3/5] Initialize database..." -ForegroundColor Cyan
    & $mysqld --defaults-file="$configFile" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) { exit 1 }
} else {
    Write-Host "[3/5] Skip initialize (data exists)." -ForegroundColor Yellow
}

Write-Host "[4/5] Install service..." -ForegroundColor Cyan
& $mysqld --remove $serviceName 2>$null
& $mysqld --install $serviceName --defaults-file="$configFile"
if ($LASTEXITCODE -ne 0) { exit 1 }

Start-Service -Name $serviceName
Start-Sleep -Seconds 3

$listening = netstat -ano | Select-String ":3306"
if (-not $listening) {
    Write-Host "[ERROR] Port 3306 not listening." -ForegroundColor Red
    exit 1
}

Write-Host "[5/5] Set root password..." -ForegroundColor Cyan
$sql = "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword'; FLUSH PRIVILEGES;"
$sql | & $mysql -u root --defaults-file="$configFile" --protocol=tcp

Write-Host ""
Write-Host "MySQL ready. Navicat: 127.0.0.1:3306 root / $rootPassword" -ForegroundColor Green
