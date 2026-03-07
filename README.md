# MediBuddy AI

基于 `Spring Boot`、`LangChain4j` 和 `Vue 3` 构建的智能医疗问诊助手示例项目。

项目面向门诊咨询与初诊分诊场景，支持：

- 医疗问答对话
- 多轮会话记忆
- 文本知识文件上传与向量化入库
- 基于 Redis 的 Embedding 存储
- MySQL 预约数据管理
- MongoDB 聊天记忆持久化

## 项目结构

```text
.
├── src/main/java                 # Spring Boot 后端源码
├── src/main/resources            # 配置、Prompt、Mapper
├── frontend                      # Vue 3 + Vite 前端
├── conf                          # 相关配置文件
├── docker-compose.yml            # 本地依赖服务编排
└── 医院信息/医生信息/科室信息等资料   # 业务知识样例文件
```

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- MyBatis-Plus
- MySQL
- MongoDB
- Redis

### 前端

- Vue 3
- TypeScript
- Vite

## 核心功能

### 1. 智能医疗问答

后端提供医疗助手接口，结合大模型完成症状咨询、就诊建议和挂号方向辅助。

- 接口：`POST /v1/medibuddy/chat`
- 请求体示例：

```json
{
  "memoryId": 1,
  "userMessage": "我发烧两天并伴随咳嗽，应该挂什么科？"
}
```

### 2. 知识文件上传

前端支持上传文本类文件，后端通过 `TextDocumentParser` 解析后写入向量库，用于后续 RAG 检索增强。

- 接口：`POST /v1/medibuddy/upload`
- 支持文本类文件，例如：`.txt`、`.md`、`.csv`、`.json`

### 3. 会话记忆

项目使用 MongoDB 持久化聊天记录，支持多轮对话上下文记忆。

### 4. 向量检索

项目使用 Redis 作为 Embedding Store，用于存储知识向量，支撑文件知识检索。

## 环境要求

启动前请准备：

- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- MongoDB 6+
- Redis 7+

## 配置说明

项目中的 `src/main/resources/application.properties` 已改为使用环境变量读取敏感配置，请先设置对应环境变量：

- `DASHSCOPE_API_KEY`
- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `MONGODB_URI`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

如果你使用默认本地环境，也可以只设置 `DASHSCOPE_API_KEY`，其余使用默认值。

## 启动方式

### 1. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

默认端口：`8888`

### 2. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

### 3. 打开页面

启动后访问前端本地地址，默认由 Vite 提供开发服务。

## Docker 服务

项目包含 `docker-compose.yml`，可用于本地拉起部分依赖服务。你可以根据自己的环境调整端口、账号和密码。

## 注意事项

- 本项目仅用于学习和演示，不能替代专业医生诊断。
- 如涉及急症，请及时前往医院或联系急救服务。
- 请勿将真实生产密钥、数据库密码直接提交到 GitHub。

## 后续可优化方向

- 增加用户登录与会话隔离
- 支持 PDF / Word 等更多格式解析
- 增强医疗知识库与检索召回策略
- 增加预约挂号完整流程
- 补充部署脚本与接口文档

## License

如需开源发布，建议补充明确的许可证，例如 `MIT`。
