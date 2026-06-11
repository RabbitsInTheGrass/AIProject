# Agent AI - 智能对话平台

基于 Spring Boot + Spring AI + React + TypeScript 的前后端分离 AI 对话平台，支持多模型接入、用户账号体系、AI 知识库、可插拔 Skill 工具系统、长期记忆。

## 项目结构

```
AIProject/
├── agent-server/     # 后端 - Spring Boot 3.5 + Spring AI + MySQL
├── agent-web/        # 前端 - React + Vite + TypeScript + Ant Design
├── agent-cli/        # 旧版 CLI 工具（可保留或删除）
├── pom.xml           # 父 POM（Maven 多模块）
└── README.md
```

## 前提条件

| 依赖 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | 17+ | Spring Boot 3.x 强制要求，需设置 `JAVA_HOME` |
| **Maven** | 3.8+ | 后端构建工具 |
| **Node.js** | 18+ | 前端构建运行时 |
| **npm** | 9+ | 前端包管理（随 Node.js 安装） |
| **MySQL** | 8.0+ | 数据库（必须，已切换为 MySQL） |

> **重要**：原项目使用 Java 8，改造后必须升级到 JDK 17+。请从 [Adoptium](https://adoptium.net/) 或 [Oracle](https://www.oracle.com/java/technologies/downloads/) 下载 JDK 17/21。

### 环境变量（可选）

```bash
# 后端启动时可通过环境变量配置（也可在 Web 界面配置）
DB_USERNAME=root                     # MySQL 用户名
DB_PASSWORD=root                     # MySQL 密码
JWT_SECRET=your-256-bit-secret-key   # JWT 签名密钥（生产环境必须修改）
AI_API_KEY=sk-your-api-key           # 默认 AI API 密钥
AI_BASE_URL=https://api.deepseek.com # 默认 AI API 地址
AI_MODEL=deepseek-chat               # 默认模型名称
APP_WORK_DIR=E:\your-project-path    # Tool 工具操作的根目录
```

## 启动方式

### 1. 准备 MySQL 数据库

```sql
-- 创建数据库
CREATE DATABASE agent_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 建表脚本（JPA 会自动创建表，此脚本仅供参考）
-- 文件位置：agent-server/src/main/resources/db/schema.sql
```

### 2. 安装前端依赖

```bash
cd agent-web
npm install
```

### 3. 启动后端

```bash
cd agent-server

# 方式一：使用环境变量
set DB_USERNAME=root
set DB_PASSWORD=root
set JWT_SECRET=your-secret-key-at-least-32-chars-long
mvn spring-boot:run

# 方式二：直接启动（使用 application.yml 中的默认配置）
mvn spring-boot:run
```

后端启动后：
- API 地址：http://localhost:8080
- **默认管理员账号：`admin` / `admin123`**（首次启动自动创建）

### 4. 启动前端

```bash
cd agent-web
npm run dev
```

前端启动后访问：http://localhost:5173

> 前端已配置 Vite 代理，`/api` 请求会自动转发到后端 `http://localhost:8080`。

### 5. 构建生产版本

```bash
# 后端打包
cd agent-server
mvn clean package
java -jar target/agent-server-2.0.0.jar

# 前端打包
cd agent-web
npm run build
# 生成 dist/ 目录，可用 Nginx 等托管
```

## 首次使用

1. **启动前后端** → 打开浏览器访问 `http://localhost:5173`
2. **登录** → 使用默认账号 `admin` / `admin123` 登录（或注册新账号）
3. **配置模型** → 点击左侧栏底部的 **「模型配置」** 按钮
4. **选择预设模板** → 点击 `+ DeepSeek`（或其他模型），自动填入 API 地址和模型名称
5. **填入 API Key** → 输入你的 API 密钥，点击确定保存
6. **开始对话** → 在输入框输入消息，按 Enter 发送

### 功能说明

| 功能 | 入口 | 说明 |
|------|------|------|
| **用户登录** | 登录页面 | 注册/登录，JWT Token 鉴权，知识库按用户隔离 |
| **多模型对话** | 顶部模型选择器 | 支持 DeepSeek/GPT/Claude/通义千问/GLM 等，界面可配置 |
| **会话管理** | 左侧栏会话列表 | 新建/切换/删除对话，历史消息自动保存 |
| **Skill 工具（内置）** | 技能管理→内置工具 | 读文件、写文件、搜索代码、执行命令等 @Tool 注解工具 |
| **Skill 工具（HTTP API）** | 技能管理→HTTP API | 页面配置外部 HTTP 工具（URL/Header/Body/参数Schema） |
| **Skill 工具（jar 插件）** | 技能管理→jar 插件 | 上传 jar 包动态加载自定义工具 |
| **知识库** | 左侧栏「知识库」 | 创建知识库，上传文档，按用户隔离 |
| **长期记忆** | 自动 | 对话后异步提取用户偏好/事实，注入后续对话上下文 |

## AI 对话链路

```
用户消息 → ChatService
  → 1. 获取/创建会话（按 userId 隔离）
  → 2. ChatModelFactory 创建 ChatModel（从数据库配置动态创建）
  → 3. SystemPromptBuilder 构建系统提示词
       ├── 注入长期记忆（LongTermMemoryService）
       └── 注入 RAG 上下文（HybridSearchService，需 Milvus+ES）
  → 4. 加载短期记忆（最近 20 条消息历史）
  → 5. SkillRegistry 组装可用工具（内置 + HTTP + 插件）
  → 6. ChatModel.stream() 流式调用（自动 Tool Calling）
  → 7. SSE 输出（content/done/error 事件）
  → 8. 异步后处理（提取长期记忆）
```

## 注意事项

### 数据库

- **MySQL**：必须安装 MySQL 8.0+，创建 `agent_platform` 数据库。JPA 的 `ddl-auto: update` 会自动建表。
- **API Key 安全**：API Key 存储在 MySQL 中，生产环境建议加密存储。

### JWT 密钥

- `application.yml` 中的 `app.jwt.secret` 是 JWT 签名密钥
- **生产环境必须修改**为至少 32 字符的随机字符串
- 修改密钥后所有已签发的 Token 会失效

### 默认管理员

- 首次启动自动创建 `admin` / `admin123` 管理员账号
- **生产环境务必修改密码**

### Milvus 向量检索（可选）

知识库目前支持文档上传和分块存储，语义检索功能需启动 Milvus 后使用：

```bash
docker run -d --name milvus-standalone -p 19530:19530 milvusdb/milvus:latest standalone
```

修改 `application.yml`：
```yaml
milvus:
  enabled: true
```

### Elasticsearch 全文检索（可选）

与 Milvus 配合实现混合检索（RRF 融合排序）：

```bash
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.12.0
```

### PowerShell 执行策略

如果 Windows PowerShell 阻止运行 npm/mvn 命令，先执行：
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### 端口冲突

- 后端默认端口 `8080`，可在 `application.yml` 中修改 `server.port`
- 前端默认端口 `5173`，可在 `vite.config.ts` 中修改 `server.port`
- 修改后端端口后，需同步更新前端 `vite.config.ts` 中的代理目标地址

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.5.10 + Spring Security |
| AI 框架 | Spring AI 1.0.6（OpenAI 兼容协议） |
| 数据库 | MySQL 8.0+ |
| 认证 | JWT (jjwt 0.12.6) + Spring Security |
| 向量检索 | Milvus（可选） |
| 全文检索 | Elasticsearch（可选） |
| 前端框架 | React 18 + TypeScript |
| 构建工具 | Vite 6 |
| UI 组件库 | Ant Design 5 |
| 状态管理 | Zustand 5 |
| 流式输出 | SSE (Server-Sent Events) |

## API 概览

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `/api/auth/**` | 注册/登录/刷新Token（公开） |
| 对话 | `/api/chat/stream` | SSE 流式对话 |
| 模型 | `/api/models/**` | 模型配置 CRUD |
| 会话 | `/api/conversations/**` | 会话管理 |
| 技能 | `/api/skills/**` | 内置技能管理 |
| HTTP 工具 | `/api/http-tools/**` | HTTP API 工具 CRUD |
| 插件 | `/api/plugins/**` | jar 插件管理 |
| 知识库 | `/api/knowledge-bases/**` | 知识库管理 + 文档上传 |
