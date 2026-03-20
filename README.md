# MediBuddy AI

基于 `Spring Boot 3`、`LangChain4j`、`Vue 3` 构建的智能医疗问诊示例项目，面向门诊咨询、初诊分诊、知识库检索与预约辅助等场景。

当前仓库已经具备后端 AI 服务、WebSocket 流式对话、知识文件上传入库、MongoDB 会话记忆、Redis 向量检索、MySQL 预约数据存储，以及一个可直接运行的前端对话界面。

## 当前项目状态

- 后端服务可运行，默认端口 `8888`
- AI 流式输出通过独立 WebSocket 服务提供，默认端口 `9000`
- 前端基于 Vite，默认开发端口 `5173`
- 已接入 DashScope/Qwen 模型、Redis Embedding Store、MongoDB 会话记忆和 MySQL 预约表
- 前端已包含登录页、聊天页、上传页
- 安全层已经启用 JWT 过滤与默认用户配置
- 当前仓库中暂未看到 `/v1/auth/login` 控制器实现，因此“登录页 -> 获取 token -> 调用受保护接口”这一段链路仍需补齐后才能完整联调

## 核心能力

### 1. WebSocket 流式医疗问答

聊天主链路已切换为 WebSocket 流式返回，前端会将模型输出持续追加到同一个消息气泡中。

- WebSocket 端点：`ws://localhost:9000`
- 鉴权方式：连接时通过 `Authorization: Bearer <token>` 或 URL 参数 `token` 传入 JWT
- 请求消息结构：

```json
{
  "memoryId": 1742460000000,
  "userMessage": "我发烧两天并伴随咳嗽，应该挂什么科？"
}
```

- 服务端响应协议：

```json
{ "type": "start", "memoryId": 1742460000000 }
{ "type": "delta", "memoryId": 1742460000000, "assistantMessage": "..." }
{ "type": "end", "memoryId": 1742460000000 }
{ "type": "error", "memoryId": 1742460000000, "error": "..." }
```

说明：

- 旧的 `POST /v1/medibuddy/chat` HTTP 接口目前在控制器中已注释，不作为当前主调用方式
- 流式对话依赖 LangChain4j `TokenStream` 与自定义 WebSocket 推送逻辑

### 2. RAG 知识文件上传

后端提供文件上传接口，接收文本文件后完成解析、切分、Embedding 计算和 Redis 入库。

- 接口：`POST /v1/medibuddy/upload`
- 控制器前缀：`/v1/medibuddy`
- 当前解析器：`TextDocumentParser`
- 当前分块策略：`recursive(500, 50)`
- 当前去重方式：按文件名维护内容 MD5，重复内容不重复入库

说明：

- 该接口在当前安全配置下默认需要认证
- 从当前代码看，更适合上传纯文本类文件，例如 `.md`、`.txt`、`.csv`、`.json`
- `pom.xml` 中已引入 PDF 解析依赖，但上传服务目前仍使用文本解析器，暂未在这条链路中启用 PDF 解析

### 3. MongoDB 会话记忆

项目通过自定义 `MongoChatMemoryStore` 持久化聊天记录，并配合 `TokenWindowChatMemory` 管理上下文窗口。

- 会话标识：`memoryId`
- 持久化存储：MongoDB
- 当前 token 窗口大小：`10000`

### 4. 预约辅助工具

AI Agent 已挂载预约相关 Tool，可辅助进行：

- 号源查询
- 创建预约记录
- 取消预约记录

底层预约数据存储在 MySQL 的 `appointment` 表中，初始化脚本见 `src/main/resources/sql/schema.sql`。

当前预约记录包含字段：

- `username`
- `id_card`
- `phone`
- `department`
- `date`
- `time`
- `doctor_name`

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- MyBatis-Plus 3.5.11
- Spring Security
- JWT（`jjwt`）
- MongoDB
- MySQL 8
- Redis Stack
- Java-WebSocket

### 前端

- Vue 3
- TypeScript
- Vite 6
- `marked`
- `dompurify`

## 项目结构

```text
.
├── src/main/java/cn/needy/medibuddy
│   ├── ai              # AI Agent、RAG、Controller、Memory Store、Tools
│   ├── security        # JWT、安全过滤器、SecurityConfig
│   ├── service         # 文件上传、预约服务
│   ├── user            # 默认内存用户仓库
│   └── websocket       # AI WebSocket 流式服务
├── src/main/resources  # application.properties、Prompt、SQL
├── frontend            # Vue 3 前端
├── conf                # 中间件配置
├── docker-compose.yml  # MongoDB / MySQL / Redis Stack
├── 医院信息.md
├── 医生信息.md
├── 科室信息.md
└── 神经内科.md
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- Docker Desktop 或本地 MySQL / MongoDB / Redis

## 配置说明

项目通过 `application.properties` 加载根目录 `.env`：

```properties
spring.config.import=optional:file:.env[.properties]
```

建议在项目根目录创建 `.env`：

```env
DASHSCOPE_API_KEY=你的阿里百炼API_KEY

MYSQL_URL=jdbc:mysql://localhost:3310/medibuddy?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
MYSQL_USERNAME=root
MYSQL_PASSWORD=root123456

MONGODB_URI=mongodb://root:123456@localhost:27017/chat_memory_db?authSource=admin

REDIS_HOST=localhost
REDIS_PORT=7001
REDIS_PASSWORD=

JWT_SECRET=change-me-to-a-long-random-secret-key-please-change
JWT_EXPIRES_DAYS=7

DEFAULT_USER=admin
DEFAULT_PASS=admin123
```

## 默认端口

- 后端 HTTP：`8888`
- AI WebSocket：`9000`
- 前端开发服务：`5173`
- MySQL：`3310`
- MongoDB：`27017`
- Redis Stack：`7001`
- RedisInsight：`8001`

## 本地启动

### 1. 启动依赖服务

```bash
docker-compose up -d
```

当前 `docker-compose.yml` 会启动：

- MongoDB
- MySQL
- Redis Stack

### 2. 初始化 MySQL 表

执行 SQL 脚本：

```text
src/main/resources/sql/schema.sql
```

默认数据库名为 `medibuddy`。

### 3. 启动后端

```bash
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

Vite 已配置代理，`/v1` 请求会转发到 `http://127.0.0.1:8888`。

## 使用说明

### 聊天功能

1. 打开前端页面
2. 如果你已经补齐了 `/v1/auth/login`，先登录获取 JWT
3. 前端会使用 token 建立到 `9000` 端口的 WebSocket 连接
4. 发送问题后，AI 回复会流式显示

### 文件上传

1. 进入上传页
2. 选择文本文件
3. 调用 `POST /v1/medibuddy/upload`
4. 后端完成解析、切分、Embedding 与 Redis 入库

## 已知事项

- 前端登录页和 JWT 安全配置已存在，但当前仓库里暂未包含 `/v1/auth/login` 控制器代码
- 上传接口和后续受保护接口在当前配置下都需要有效 JWT
- Redis 端口当前以 `7001` 为准，早期 README 中的 `6380` 已不再匹配当前配置
- 仓库中存在一些中文注释/文档编码历史问题，若终端编码不是 UTF-8，查看文件时可能出现乱码

## 后续可继续完善

- 补齐认证控制器与完整登录流程
- 将上传链路扩展为 PDF / Word 等更多文档格式
- 增强 RAG 检索策略与召回调优
- 增加预约号源的真实校验逻辑
- 补充接口文档与部署说明

## License

本项目采用 `MIT License`。
