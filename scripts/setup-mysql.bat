@echo off
chcp 65001 >nul
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Please run as Administrator.
    echo Right-click this file -^> Run as administrator
    pause
    exit /b 1
)

set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 9.2\bin
set DATA_DIR=C:\ProgramData\MySQL\MySQL Server 9.2\Data
set CONFIG_DIR=C:\ProgramData\MySQL\MySQL Server 9.2
set CONFIG_FILE=%CONFIG_DIR%\my.ini
set SERVICE_NAME=MySQL92
set ROOT_PWD=xueren123

if not exist "%MYSQL_BIN%\mysqld.exe" (
    echo [ERROR] MySQL not found: %MYSQL_BIN%\mysqld.exe
    pause
    exit /b 1
)

echo [1/5] Create directories...
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%CONFIG_DIR%" mkdir "%CONFIG_DIR%"

echo [2/5] Copy config file...
copy /Y "%~dp0my.ini" "%CONFIG_FILE%" >nul

echo [3/5] Initialize database (first run only)...
if not exist "%DATA_DIR%\mysql" (
    "%MYSQL_BIN%\mysqld.exe" --defaults-file="%CONFIG_FILE%" --initialize-insecure --console
    if errorlevel 1 (
        echo [ERROR] Initialize failed.
        pause
        exit /b 1
    )
) else (
    echo Data directory exists, skip initialize.
)

echo [4/5] Install and start Windows service...
"%MYSQL_BIN%\mysqld.exe" --remove %SERVICE_NAME% >nul 2>&1
"%MYSQL_BIN%\mysqld.exe" --install %SERVICE_NAME% --defaults-file="%CONFIG_FILE%"
if errorlevel 1 (
    echo [ERROR] Install service failed.
    pause
    exit /b 1
)
net start %SERVICE_NAME%
timeout /t 3 /nobreak >nul

netstat -ano | findstr ":3306" >nul
if errorlevel 1 (
    echo [ERROR] Port 3306 not listening. Check Event Viewer.
    pause
    exit /b 1
)

echo [5/5] Set root password...
echo ALTER USER 'root'@'localhost' IDENTIFIED BY '%ROOT_PWD%'; FLUSH PRIVILEGES; | "%MYSQL_BIN%\mysql.exe" -u root --defaults-file="%CONFIG_FILE%" --protocol=tcp

echo.
echo ========================================
echo  MySQL is ready!
echo ========================================
echo  Navicat:
echo    Host:     127.0.0.1
echo    Port:     3306
echo    User:     root
echo    Password: %ROOT_PWD%
echo.
echo  Import database:
echo    "%MYSQL_BIN%\mysql.exe" -u root -p ^< d:\trae\xueren\sql\init.sql
echo    (password: %ROOT_PWD%)
echo.
pause
