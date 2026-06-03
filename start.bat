@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  雪人 (Xueren) — 一键启动脚本 (Windows)
::  用法:
::    start.bat             启动全部服务
::    start.bat build       先 Maven 编译打包，再启动
::    start.bat stop        停止所有服务
::    start.bat restart     重启所有服务
:: ============================================================

title 雪人 - 一键启动
cd /d "%~dp0"

:: ==================== 可配置项 ====================
set "MYSQL_SERVICE=MySQL92"
set "BACKEND_JAR=backend\target\xueren-backend-1.0.0.jar"
set "HEALTH_URL=http://localhost:8080/health"
set "FRONTEND_URL=http://localhost:5173"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "BACKEND_TIMEOUT=60"
set "FRONTEND_TIMEOUT=30"
:: ==================================================

:: 加载 .env（如果存在）
if exist ".env" (
    for /f "usebackq eol=# tokens=1,2 delims==" %%a in (".env") do (
        if not "%%a"=="" if not "%%b"=="" set "%%a=%%b" 2>nul
    )
)

:: ==================== 命令分发 ====================
if /i "%~1"=="stop"    goto :cmd_stop
if /i "%~1"=="restart" goto :cmd_restart
if /i "%~1"=="build"   set "DO_BUILD=1"

goto :main

:: ==================== 主流程 ====================
:main

echo.
echo   ╔══════════════════════════════════╗
echo   ║       雪人 Xueren 一键启动       ║
echo   ╚══════════════════════════════════╝
echo.

:: ---------- 0. Maven 构建（可选）----------
if "%DO_BUILD%"=="1" (
    echo [构建] Maven 编译打包...
    call :maven_build
    if errorlevel 1 (
        echo   [失败] Maven 构建失败！
        pause
        exit /b 1
    )
    echo   构建完成 ✓
    echo.
)

:: ---------- 1. MySQL ----------
echo [1/3] 检查 MySQL...
call :ensure_mysql
if errorlevel 1 (
    pause
    exit /b 1
)

:: ---------- 2. 后端 ----------
echo [2/3] 启动后端 (Spring Boot)...
call :start_backend

:: ---------- 3. 前端 ----------
echo [3/3] 启动前端 (Vue 3 + Vite)...
call :start_frontend

:: ---------- 完成 ----------
:done
echo.
echo   ══════════════════════════════════════════
echo   启动完成！
echo.
echo    前端:      %FRONTEND_URL%
echo    后端 API:  http://localhost:%BACKEND_PORT%
echo    健康检查:  %HEALTH_URL%
echo    WebSocket: ws://localhost:8081/ws
echo.
echo   服务已脱离控制台运行，可以安全关闭本窗口。
echo   关闭服务请运行: start.bat stop
echo.
echo   按任意键打开前端页面...
pause >nul
start %FRONTEND_URL%
exit /b 0


:: ==================== 子过程 ====================

:: --- Maven 构建 ---
:maven_build
:: 优先用 Maven Wrapper，找不到就用系统 mvn
if exist "backend\mvnw.cmd" (
    cd backend
    call mvnw.cmd clean package -DskipTests -q
    set "MVN_EXIT=!errorlevel!"
    cd ..
    exit /b !MVN_EXIT!
)
if exist "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests -q -f backend\pom.xml
    exit /b !errorlevel!
)
where mvn.cmd >nul 2>&1
if not errorlevel 1 (
    cd backend
    call mvn clean package -DskipTests -q
    set "MVN_EXIT=!errorlevel!"
    cd ..
    exit /b !MVN_EXIT!
)
echo   未找到 Maven，跳过构建（请手动构建或安装 Maven）
exit /b 0

:: --- 确保 MySQL 运行 ---
:ensure_mysql
:: 先检查进程是否存在
tasklist /fi "imagename eq mysqld.exe" 2>nul | find "mysqld.exe" >nul
if not errorlevel 1 (
    echo   MySQL ✓ (进程已运行)
    exit /b 0
)
:: 尝试启动配置的服务
net start "%MYSQL_SERVICE%" >nul 2>&1
if not errorlevel 1 (
    echo   MySQL ✓ (服务 %MYSQL_SERVICE% 已启动)
    exit /b 0
)
:: 尝试常见服务名
for %%s in (MySQL MySQL80 MySQL84 MySQL90 MySQL57 MySQL8.0) do (
    net start %%s >nul 2>&1
    if not errorlevel 1 (
        set "MYSQL_SERVICE=%%s"
        echo   MySQL ✓ (服务 %%s 已启动)
        exit /b 0
    )
)
echo   [失败] 无法启动 MySQL 服务。
echo         已尝试的服务名: %MYSQL_SERVICE%, MySQL, MySQL80, MySQL84, MySQL90, MySQL57, MySQL8.0
echo         请修改脚本顶部 MYSQL_SERVICE 变量，或手动启动 MySQL 后重试。
exit /b 1

:: --- 启动后端 ---
:start_backend
:: 检查 JAR
if not exist "%BACKEND_JAR%" (
    echo   [错误] 找不到 %BACKEND_JAR%
    echo         请先在 IDE 中构建，或运行: start.bat build
    exit /b 1
)
:: 检查端口
call :port_in_use %BACKEND_PORT%
if "!PORT_IN_USE!"=="1" (
    echo   端口 %BACKEND_PORT% 已被占用 — 后端可能已在运行。
    exit /b 0
)
:: 通过 VBScript 启动（完全脱离控制台，关窗口不会停）
echo   后台启动中（脱离控制台）...
(
echo Set ws = CreateObject("WScript.Shell"^)
echo ws.Run "cmd /c cd /d %CD%\backend && java -jar target\xueren-backend-1.0.0.jar", 0, False
) > "%TEMP%\xueren_backend.vbs"
cscript //nologo "%TEMP%\xueren_backend.vbs" >nul 2>&1
del "%TEMP%\xueren_backend.vbs" 2>nul
call :wait_url "%HEALTH_URL%" %BACKEND_TIMEOUT% "后端"
if errorlevel 1 (
    echo   [警告] 后端启动超时（等待了 %BACKEND_TIMEOUT% 秒）
)
exit /b 0

:: --- 启动前端 ---
:start_frontend
:: 检查端口
call :port_in_use %FRONTEND_PORT%
if "!PORT_IN_USE!"=="1" (
    echo   端口 %FRONTEND_PORT% 已被占用 — 前端可能已在运行。
    exit /b 0
)
:: 安装依赖（如果需要）
if not exist "frontend\node_modules" (
    echo   安装前端依赖...
    cd frontend
    call npm install
    cd ..
)
:: 通过 VBScript 启动（完全脱离控制台，关窗口不会停）
echo   后台启动中（脱离控制台）...
(
echo Set ws = CreateObject("WScript.Shell"^)
echo ws.Run "cmd /c cd /d %CD%\frontend && npx vite --host 0.0.0.0 --port %FRONTEND_PORT%", 0, False
) > "%TEMP%\xueren_frontend.vbs"
cscript //nologo "%TEMP%\xueren_frontend.vbs" >nul 2>&1
del "%TEMP%\xueren_frontend.vbs" 2>nul
call :wait_url "%FRONTEND_URL%" %FRONTEND_TIMEOUT% "前端"
if errorlevel 1 (
    echo   [警告] 前端启动超时（等待了 %FRONTEND_TIMEOUT% 秒）
)
exit /b 0

:: --- 检查端口是否被占用 ---
:: 设置 PORT_IN_USE=1 表示被占用
:port_in_use
set "PORT_IN_USE=0"
netstat -ano 2>nul | findstr ":%~1.*LISTENING" >nul
if not errorlevel 1 set "PORT_IN_USE=1"
exit /b 0

:: --- 等待 URL 可访问 ---
:: 参数: %1=URL  %2=超时(秒)  %3=显示名称
:wait_url
setlocal
set "U=%~1"
set "TO=%~2"
set "NM=%~3"
set /a "ELAPSED=0"
<nul set /p "=  等待%NM%启动"
:wait_loop
timeout /t 1 /nobreak >nul
set /a "ELAPSED+=1"
curl -s -o NUL "%U%" 2>nul
if not errorlevel 1 (
    echo.
    echo   %NM% ✓ (%U%)
    endlocal & exit /b 0
)
<nul set /p "=."
if !ELAPSED! lss %TO% goto wait_loop
echo.
endlocal & exit /b 1


:: ==================== 停止服务 ====================
:cmd_stop
echo.
echo   正在停止 雪人 服务...
echo.

:: 通过端口杀进程（wmic 启动的进程没有窗口，只能按端口杀）
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%BACKEND_PORT%.*LISTENING"') do (
    echo   停止后端 (端口 %BACKEND_PORT%, PID: %%a)...
    taskkill /pid %%a /f >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%FRONTEND_PORT%.*LISTENING"') do (
    echo   停止前端 (端口 %FRONTEND_PORT%, PID: %%a)...
    taskkill /pid %%a /f >nul 2>&1
)

echo   已停止所有服务。
exit /b 0


:: ==================== 重启 ====================
:cmd_restart
call :cmd_stop
echo   等待 3 秒后重新启动...
timeout /t 3 /nobreak >nul
:: 保留 build 参数
if /i "%~2"=="build" set "DO_BUILD=1"
goto :main
\r