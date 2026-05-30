# 雪人 (Xueren) - 简易类微信 Web 应用

## 项目结构

```
xueren/
├── sql/           # 数据库脚本
├── scripts/       # MySQL 安装脚本
├── backend/       # Spring Boot + Netty
└── frontend/      # Vue 3 前端
```

## 启动顺序

### 1. MySQL

确保 MySQL 运行，并已执行 `sql/init.sql`。

### 2. 后端

```bash
cd backend
# IDEA 运行 XuerenApplication，或：
java -jar target/xueren-backend-1.0.0.jar
```

- HTTP API：http://localhost:8080
- WebSocket：ws://localhost:8081/ws?token=你的JWT

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

打开：http://localhost:5173

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3、JPA、JWT、Netty WebSocket |
| 前端 | Vue 3、Vite、Pinia、Element Plus |
| 数据库 | MySQL 8 |
