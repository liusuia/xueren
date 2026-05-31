#!/bin/bash
# 雪人 Xueren - 一键启动 (Bash / Git Bash / WSL)

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 9.2/bin"
SERVICE_NAME="MySQL92"

echo ""
echo "  ╔══════════════════════════════════╗"
echo "  ║     雪人 Xueren 一键启动        ║"
echo "  ╚══════════════════════════════════╝"
echo ""

# ---------- 1. MySQL ----------
echo "[1/3] 检查 MySQL..."
sc query "$SERVICE_NAME" | grep -q "RUNNING" 2>/dev/null || {
    echo "  启动 MySQL 服务..."
    net start "$SERVICE_NAME" 2>/dev/null || {
        echo "  [失败] 无法启动 MySQL"
        exit 1
    }
}
echo "  MySQL  ✓"

# ---------- 2. 后端 ----------
echo "[2/3] 启动后端 (Spring Boot)..."
java -jar backend/target/xueren-backend-1.0.0.jar &
BACKEND_PID=$!

# 等待后端就绪
echo -n "  等待后端启动"
for i in $(seq 1 30); do
    sleep 1
    curl -s http://localhost:8080/api/health >/dev/null 2>&1 && {
        echo ""
        echo "  后端 ✓ (http://localhost:8080)"
        break
    }
    echo -n "."
done

# ---------- 3. 前端 ----------
echo "[3/3] 启动前端 (Vue 3)..."
cd frontend && npx vite --host 0.0.0.0 --port 5173 &
FRONTEND_PID=$!

# 等待前端就绪
echo -n "  等待前端启动"
for i in $(seq 1 15); do
    sleep 1
    curl -s http://localhost:5173 >/dev/null 2>&1 && {
        echo ""
        echo "  前端 ✓ (http://localhost:5173)"
        break
    }
    echo -n "."
done

# ---------- 完成 ----------
echo ""
echo "  ═══════════════════════════════════"
echo "  全部启动完成！"
echo ""
echo "   前端: http://localhost:5173"
echo "   后端: http://localhost:8080"
echo "   WS:   ws://localhost:8081/ws"
echo ""
echo "  Ctrl+C 停止所有服务"

# 等待子进程
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
