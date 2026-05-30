# 雪人聊天 - 前端

Vue 3 + Vite + Element Plus

## 启动

```bash
cd frontend
npm install
npm run dev
```

浏览器打开：http://localhost:5173

## 环境

- 后端 HTTP：`http://localhost:8080`
- WebSocket：`ws://localhost:8081/ws`

可在 `.env` 中配置：

```
VITE_WS_URL=ws://localhost:8081
```

## 使用说明

1. 注册两个账号（如 alice / bob）
2. 互相搜索并添加好友，对方在「好友申请」里同意
3. 点击好友或会话开始聊天，消息经 WebSocket 实时推送
