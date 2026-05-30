# Xueren 后端

Spring Boot 3 + MySQL + JWT，提供类微信应用 REST API。

## 环境要求

- JDK 17+（你当前是 Java 24，可用）
- MySQL 8+（库名 `xueren`，已执行 `../sql/init.sql`）
- Maven 3.8+（或用 IntelliJ IDEA 自带 Maven）

## 配置

数据库连接见 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/xueren
    username: root
    password: xueren123
```

## 启动

### 方式一：IntelliJ IDEA（推荐）

1. **File → Open** 打开 `backend` 文件夹
2. 等待 Maven 依赖下载完成
3. 运行 `com.xueren.XuerenApplication`

### 方式二：命令行

```bash
cd backend
mvn spring-boot:run
```

启动成功后访问：

- 首页（健康检查）：`http://localhost:8080/` 或 `http://localhost:8080/health`
- **不要用浏览器打开** `/api/auth/register` 等接口（浏览器只会发 GET，会报错）
- 注册/登录请用 **Postman**，Body 选 JSON，方法选 **POST**

> 根路径以前会显示「拒绝访问」，是因为 Spring Security 要求登录；现已开放 `/` 和 `/health`。

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/refresh` | 刷新 Token |
| GET | `/api/users/me` | 当前用户 |
| GET | `/api/users/search?keyword=` | 搜索用户 |
| GET | `/api/friends` | 好友列表 |
| GET | `/api/friends/requests` | 待处理好友申请 |
| POST | `/api/friends/request` | 发送好友申请 |
| POST | `/api/friends/accept/{requesterId}` | 同意申请 |
| POST | `/api/friends/reject/{requesterId}` | 拒绝申请 |
| POST | `/api/friends/block/{friendId}` | 拉黑 |
| PUT | `/api/friends/{friendId}/remark` | 设置备注 |
| GET | `/api/conversations` | 会话列表 |
| POST | `/api/conversations/read` | 标记会话已读 |
| POST | `/api/messages` | 发送消息 |
| GET | `/api/messages/single/{peerId}` | 单聊历史 |
| GET | `/api/messages/group/{groupId}` | 群聊历史 |
| POST | `/api/messages/{id}/recall` | 撤回消息 |
| POST | `/api/messages/{id}/read` | 消息已读 |
| POST | `/api/groups` | 创建群 |
| GET | `/api/groups` | 我的群列表 |
| POST | `/api/files/upload` | 上传文件 |

除 `/api/auth/**` 和 `/uploads/**` 外，请求需带 Header：

```
Authorization: Bearer <accessToken>
```

## 快速测试（PowerShell）

```powershell
# 注册
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register `
  -ContentType "application/json" `
  -Body '{"username":"alice","password":"123456","nickname":"Alice"}'

# 登录
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"alice","password":"123456"}'
$token = $r.data.accessToken

# 获取当前用户
Invoke-RestMethod -Uri http://localhost:8080/api/users/me `
  -Headers @{ Authorization = "Bearer $token" }
```

## WebSocket（Netty）

- 地址：`ws://localhost:8081/ws?token=<accessToken>`
- 发消息：`{"type":"CHAT","data":{"chatType":1,"toUserId":2,"msgType":1,"content":"你好"}}`
- 心跳：`{"type":"PING"}` → 服务端回复 `PONG`
- 推送：`NEW_MESSAGE`、`MESSAGE_RECALLED`

## 前端

见项目根目录 `frontend/`，`npm install && npm run dev` 后访问 http://localhost:5173
