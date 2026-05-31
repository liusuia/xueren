@echo off
chcp 65001 >nul
title 轻语 - 一键启动

echo.
echo   ╔══════════════════════════════════╗
echo   ║     轻语 一键启动        ║
echo   ╚══════════════════════════════════╝
echo.

cd /d "%~dp0"

set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 9.2\bin
set SERVICE_NAME=MySQL92

:: ---------- 1. MySQL ----------
echo [1/3] 检查 MySQL...
sc query %SERVICE_NAME% | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo   启动 MySQL 服务...
    net start %SERVICE_NAME% >nul 2>&1
    if %errorlevel% neq 0 (
        echo   [失败] 无法启动 MySQL，请手动检查。
        pause
        exit /b 1
    )
)
echo    MySQL  ✓

:: ---------- 2. 后端 ----------
echo [2/3] 启动后端 (Spring Boot)...
start "Xueren-Backend" cmd /c "cd /d backend && java -jar target\xueren-backend-1.0.0.jar"

:: 等待后端就绪
echo   等待后端启动...
for /L %%i in (1,1,30) do (
    timeout /t 1 /nobreak >nul
    curl -s http://localhost:8080/api/health >nul 2>&1
    if not errorlevel 1 goto backend_ready
    echo|set /p="."
)
echo.
echo   [警告] 后端启动超时，请检查 backend 窗口。
goto frontend

:backend_ready
echo   后端 ✓ (http://localhost:8080)

:: ---------- 3. 前端 ----------
:frontend
echo [3/3] 启动前端 (Vue 3)...
start "Xueren-Frontend" cmd /c "cd /d frontend && npx vite --host 0.0.0.0 --port 5173"

:: 等待前端就绪
echo   等待前端启动...
for /L %%i in (1,1,15) do (
    timeout /t 1 /nobreak >nul
    curl -s http://localhost:5173 >nul 2>&1
    if not errorlevel 1 goto frontend_ready
    echo|set /p="."
)
echo.
echo   [警告] 前端启动超时。
goto done

:frontend_ready
echo   前端 ✓ (http://localhost:5173)

:: ---------- 完成 ----------
:done
echo.
echo   ═══════════════════════════════════
echo   全部启动完成！
echo.
echo    前端: http://localhost:5173
echo    后端: http://localhost:8080
echo    WS:   ws://localhost:8081/ws
echo.
echo   按任意键打开前端页面...
pause >nul
start http://localhost:5173
