# MediBuddy AI

基于 `Spring Boot`、`LangChain4j` 和 `Vue 3` 构建的智能医疗问诊助手示例项目。

项目面向门诊咨询与初诊分诊场景，支持：

- 医疗问答对话
- 多轮会话记忆
- 文本知识文件上传与向量化入库
- 基于 Redis 的 Embedding 存储
- MySQL 预约数据管理
- MongoDB 聊天记忆持久化
- 科室与医生知识文件检索增强

## 项目结构

```text
.
├── src/main/java                 # Spring Boot 后端源码
├── src/main/resources            # 配置、Prompt、Mapper、SQL
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

### 2. 知识文件上传与 RAG 检索

前端支持上传文本类文件，后端通过 `TextDocumentParser` 解析后写入向量库，用于后续 RAG 检索增强。

- 接口：`POST /v1/medibuddy/upload`
- 支持文本类文件，例如：`.txt`、`.md`、`.csv`、`.json`
- 当前已对 RAG 检索阈值做调整，便于提升知识召回效果

### 3. 会话记忆

项目使用 MongoDB 持久化聊天记录，支持多轮对话上下文记忆。

### 4. 预约信息管理

预约实体已补充手机号字段，并将就诊日期、时间调整为更明确的类型：

- `phone`
- `date -> LocalDate`
- `time -> LocalTime`

数据库初始化脚本位于：`src/main/resources/sql/schema.sql`

## 环境要求

启动前请准备：

- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- MongoDB 6+
- Redis 7+

## 配置说明

项目使用 `.env` + `application.properties` 的方式加载敏感配置。

### 1. 本地 `.env`

在项目根目录创建 `.env` 文件：

```env
DASHSCOPE_API_KEY=你的阿里百炼API_KEY
MYSQL_URL=jdbc:mysql://localhost:3310/medibuddy?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
MYSQL_USERNAME=root
MYSQL_PASSWORD=root123456
MONGODB_URI=mongodb://root:123456@localhost:27017/chat_memory_db?authSource=admin
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=
```

### 2. 配置加载说明

`src/main/resources/application.properties` 已通过以下配置自动导入项目根目录 `.env`：

```properties
spring.config.import=optional:file:.env[.properties]
```

### 3. 默认数据库与端口

- 后端端口：`8888`
- MySQL：`localhost:3310`
- MySQL 数据库名：`medibuddy`
- MongoDB：`localhost:27017`
- Redis：`localhost:6380`

## 数据库初始化

项目提供预约表初始化脚本：

- `src/main/resources/sql/schema.sql`

如果你使用 Docker 启动 MySQL，请先创建或确认数据库名为 `medibuddy`，再执行上面的 SQL 脚本。

## 启动方式

### 1. 启动依赖服务

可以直接使用：

```bash
docker-compose up -d
```

### 2. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

### 3. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

### 4. 打开页面

启动后访问前端本地地址，默认由 Vite 提供开发服务。

## 最近调整

本次更新包含以下内容：

- 新增 `.env` 配置加载方式，避免将 API Key 直接写入仓库
- MySQL 默认数据库名统一调整为 `medibuddy`
- 新增预约表 SQL 初始化脚本
- 预约实体增加手机号字段，并优化日期时间类型
- 微调前端应用标题文案
- 调整 RAG 检索阈值并增加调试日志输出
- 优化神经内科与医生知识文件内容组织

## 注意事项

- 本项目仅用于学习和演示，不能替代专业医生诊断
- 如涉及急症，请及时前往医院或联系急救服务
- 请勿将真实生产密钥、数据库密码直接提交到 GitHub
- `.env` 已被忽略，不会默认上传到远端仓库

## 后续可优化方向

- 增加用户登录与会话隔离
- 支持 PDF / Word 等更多格式解析
- 增强医疗知识库与检索召回策略
- 增加预约挂号完整流程
- 补充部署脚本与接口文档

## License

如需开源发布，建议补充明确的许可证，例如 `MIT`。
