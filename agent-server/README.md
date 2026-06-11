# Agent Server — 后端服务

基于 Spring Boot + Spring AI 的 AI 对话平台后端服务，提供多模型接入、用户认证、SSE 流式对话、可插拔 Skill 工具系统、AI 知识库、长期记忆等能力。

---

## 技术架构

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.5.10 |
| AI 框架 | Spring AI (OpenAI 兼容协议) | 1.0.6 |
| 安全认证 | Spring Security + JWT (jjwt) | 0.12.6 |
| 数据持久化 | Spring Data JPA + MySQL | 8.0+ |
| 文档解析 | Apache Tika (Spring AI 集成) | - |
| 向量检索 | Milvus (可选，开发阶段禁用) | - |
| 全文检索 | Elasticsearch (可选) | - |
| 流式输出 | SSE (SseEmitter) | - |
| 构建工具 | Maven | 3.8+ |
| JDK | Java | 17+ |

---

## 文件结构

```
agent-server/
├── pom.xml                              # Maven 依赖配置
├── src/main/
│   ├── java/com/agent/server/
│   │   ├── AgentServerApplication.java  # 启动入口（排除 Milvus 自动配置）
│   │   │
│   │   ├── chat/                        # AI 对话核心
│   │   │   ├── ChatModelFactory.java    # 动态 ChatModel 工厂（从数据库配置创建，ConcurrentHashMap 缓存）
│   │   │   └── SystemPromptBuilder.java # 系统提示词构建器（注入长期记忆 + Skill 列表）
│   │   │
│   │   ├── config/                      # 配置类
│   │   │   ├── WebConfig.java           # CORS 跨域配置
│   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   │   └── DataInitializer.java     # 启动时初始化（默认管理员 + 内置技能）
│   │   │
│   │   ├── controller/                  # REST API 控制器
│   │   │   ├── AuthController.java      # POST /api/auth/register, /login, /refresh, GET /me
│   │   │   ├── ChatController.java      # POST /api/chat/stream (SSE)
│   │   │   ├── ConversationController.java  # 会话 CRUD + 消息列表
│   │   │   ├── ModelConfigController.java   # 模型配置 CRUD + 预设模板
│   │   │   ├── SkillController.java     # 内置技能列表 + 启用/禁用
│   │   │   ├── KnowledgeBaseController.java # 知识库管理 + 文档上传
│   │   │   ├── HttpToolController.java  # HTTP API 工具 CRUD
│   │   │   └── PluginToolController.java    # jar 插件管理
│   │   │
│   │   ├── model/
│   │   │   ├── entity/                  # 13 个 JPA 实体（均含 userId 多租户隔离）
│   │   │   │   ├── SysUser.java         # 用户（LOCAL/EMAIL/GITHUB/GOOGLE 多种 authProvider）
│   │   │   │   ├── SysUserRole.java     # 用户角色（ADMIN/USER）
│   │   │   │   ├── Conversation.java    # 对话会话
│   │   │   │   ├── ChatMessageEntity.java   # 对话消息
│   │   │   │   ├── ModelConfig.java     # 模型配置（baseUrl/apiKey/modelName/temperature）
│   │   │   │   ├── SkillConfig.java     # 内置技能配置
│   │   │   │   ├── HttpToolConfig.java  # HTTP API 工具配置
│   │   │   │   ├── PluginTool.java      # jar 插件工具
│   │   │   │   ├── KnowledgeBase.java   # 知识库
│   │   │   │   ├── KnowledgeDocument.java   # 知识库文档
│   │   │   │   ├── UserLongTermMemory.java  # 用户长期记忆
│   │   │   │   └── VerificationCode.java    # 验证码
│   │   │   └── dto/                     # 8 个 DTO
│   │   │       ├── LoginRequest.java, RegisterRequest.java, AuthResponse.java
│   │   │       ├── ChatRequest.java, ChatStreamEvent.java
│   │   │       ├── ModelConfigDTO.java, HttpToolConfigDTO.java, PluginToolDTO.java
│   │   │
│   │   ├── repository/                  # 12 个 Spring Data JPA Repository
│   │   │   ├── SysUserRepository.java   # findByUsername/findByEmail/existsByUsername 等
│   │   │   ├── ConversationRepository.java  # findByUserIdOrderByUpdatedAtDesc
│   │   │   ├── ModelConfigRepository.java   # findByUserId/findByIsDefaultTrue
│   │   │   ├── HttpToolConfigRepository.java, PluginToolRepository.java
│   │   │   ├── UserLongTermMemoryRepository.java, VerificationCodeRepository.java
│   │   │   └── ... (其余按实体对应)
│   │   │
│   │   ├── security/                    # JWT 认证链
│   │   │   ├── JwtTokenProvider.java    # Token 生成/验证/解析（access + refresh 双 Token）
│   │   │   ├── JwtAuthFilter.java       # OncePerRequestFilter 拦截请求验证 JWT
│   │   │   ├── SecurityConfig.java      # Spring Security 配置（无状态 + 公开路径白名单）
│   │   │   ├── UserPrincipal.java       # UserDetails 实现（含 userId + roles）
│   │   │   └── SecurityUtil.java        # 静态工具类获取当前用户
│   │   │
│   │   ├── service/                     # 9 个 Service
│   │   │   ├── ChatService.java         # 核心：流式对话（SseEmitter + ChatModel.stream + Tool Calling）
│   │   │   ├── AuthService.java         # 注册/登录/刷新 Token
│   │   │   ├── ConversationService.java # 会话管理（getOrCreate + 按 userId 隔离）
│   │   │   ├── ModelConfigService.java  # 模型配置 CRUD + 预设模板列表
│   │   │   ├── KnowledgeBaseService.java    # 知识库 + Tika 文档解析 + 分块存储
│   │   │   ├── HttpToolService.java     # HTTP API 工具 CRUD
│   │   │   ├── PluginToolService.java   # jar 插件 CRUD + 文件管理
│   │   │   ├── LongTermMemoryService.java   # 长期记忆（异步提取 + 注入上下文）
│   │   │   └── HybridSearchService.java # 混合检索 stub（Milvus + ES，待启用）
│   │   │
│   │   └── skill/                       # 可插拔工具系统
│   │       ├── FileTools.java           # @Tool: readFile/writeFile/listDirectory/findFiles
│   │       ├── CodeSearchTools.java     # @Tool: searchCode/searchDefinition
│   │       ├── ShellTools.java          # @Tool: executeShell/getSystemInfo
│   │       └── SkillRegistry.java       # 工具注册中心（MethodToolCallbackProvider）
│   │
│   └── resources/
│       ├── application.yml              # 应用配置
│       └── db/schema.sql                # MySQL 建表脚本（JPA 自动建表，此文件供参考）
```

---

## 核心功能详解

### 1. 用户认证 (JWT)

- **注册/登录**: 用户名 + 密码，BCrypt 加密存储
- **双 Token**: accessToken (24h) + refreshToken (7d)
- **鉴权流程**: 请求 → JwtAuthFilter → 解析 Bearer Token → 加载 UserPrincipal → SecurityContext
- **公开路径**: `/api/auth/**` 和 `OPTIONS` 请求不需认证
- **默认管理员**: 首次启动自动创建 `admin` / `admin123`（由 DataInitializer 完成）

**配置** (`application.yml`):
```yaml
app.jwt:
  secret: ${JWT_SECRET:...}       # 签名密钥（生产必须修改，至少32字符）
  expiration-hours: 24             # accessToken 有效期
  refresh-expiration-hours: 168    # refreshToken 有效期
```

### 2. 多模型动态切换

- **ChatModelFactory**: 根据数据库 ModelConfig 记录动态创建 OpenAiChatModel 实例，ConcurrentHashMap 缓存
- **配置更新**: 修改模型配置时自动 evict 缓存，下次请求重建
- **预设模板**: 内置 DeepSeek / GPT-4o / Claude / 通义千问 / GLM-4 / Moonshot 6 种预设

**配置**: 所有模型参数通过 Web 界面配置到数据库，包括 baseUrl、apiKey、modelName、temperature、maxTokens

### 3. SSE 流式对话

- **ChatService.streamChat()**: 使用 SseEmitter (5min timeout) + ChatModel.stream()
- **事件类型**: `content`（增量文本）、`done`（完成+conversationId）、`error`（错误信息）
- **消息历史**: 最近 20 条消息作为上下文（短期记忆）
- **虚拟线程**: 每个请求启动 VirtualThread 避免阻塞

### 4. Tool Calling (函数调用)

- **@Tool 注解**: 使用 Spring AI 的 `@Tool` + `@ToolParam` 注解定义工具方法
- **MethodToolCallbackProvider**: 自动将 @Tool 方法注册为 ToolCallback
- **自动调用**: LLM 在对话中自动决定是否调用工具，Spring AI 处理工具调用循环

**内置工具 (8个)**:
| 工具 | 方法 | 说明 |
|------|------|------|
| FileTools | readFile, writeFile, listDirectory, findFiles | 文件读写、目录列表、glob 搜索 |
| CodeSearchTools | searchCode, searchDefinition | 正则代码搜索、定义查找 |
| ShellTools | executeShell, getSystemInfo | Shell 命令执行、系统信息 |

### 5. 可插拔 Skill 系统 (三种方式)

| 方式 | 说明 | 配置入口 |
|------|------|---------|
| **内置工具** | @Tool 注解，编译时确定 | 前端「技能管理→内置工具」Tab 启用/禁用 |
| **HTTP API 工具** | 页面配置 URL/Header/Body 模板/参数 Schema | 前端「技能管理→HTTP API」Tab 添加/编辑/删除 |
| **jar 插件** | 上传 jar + 指定 mainClass，动态加载 | 前端「技能管理→jar 插件」Tab 上传 |

### 6. AI 知识库

- **文档上传**: 支持任意格式文件（Tika 自动解析）
- **分块存储**: 文档解析后按段落分块存入 MySQL
- **混合检索 (stub)**: Milvus 向量检索 + ES BM25 关键词检索 + RRF 融合排序（需启用 Milvus/ES）
- **按用户隔离**: 每个用户只能访问自己创建的知识库

### 7. 长期记忆

- **异步提取**: 对话后 @Async 提取用户偏好/事实到 UserLongTermMemory 表
- **注入上下文**: SystemPromptBuilder 将活跃记忆注入系统提示词
- **记忆类型**: PREFERENCE（偏好）、FACT（事实）、SUMMARY（摘要）

---

## application.yml 配置项

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|---------|--------|------|
| `server.port` | - | 8080 | 服务端口 |
| `spring.datasource.url` | - | localhost:3306/agent_platform | MySQL 连接 |
| `spring.datasource.username` | `DB_USERNAME` | root | 数据库用户名 |
| `spring.datasource.password` | `DB_PASSWORD` | root | 数据库密码 |
| `spring.jpa.hibernate.ddl-auto` | - | update | 自动建表策略 |
| `spring.ai.openai.api-key` | `AI_API_KEY` | sk-placeholder | 默认 AI API 密钥 |
| `spring.ai.openai.base-url` | `AI_BASE_URL` | https://api.deepseek.com | 默认 AI 地址 |
| `spring.ai.openai.chat.options.model` | `AI_MODEL` | deepseek-chat | 默认模型名称 |
| `milvus.enabled` | - | false | 是否启用 Milvus 向量检索 |
| `milvus.host` | `MILVUS_HOST` | localhost | Milvus 地址 |
| `milvus.port` | `MILVUS_PORT` | 19530 | Milvus 端口 |
| `app.jwt.secret` | `JWT_SECRET` | (内置默认值) | JWT 签名密钥 |
| `app.jwt.expiration-hours` | - | 24 | accessToken 有效期(小时) |
| `app.work-dir` | `APP_WORK_DIR` | ./workspace | Tool 工具操作根目录 |
| `app.cors.allowed-origins` | - | localhost:5173,3000 | CORS 允许的前端域 |
| `app.plugins.upload-dir` | `PLUGIN_DIR` | ./plugins | jar 插件上传目录 |
| `app.search.es-enabled` | `ES_ENABLED` | false | 是否启用 ES 全文检索 |

---

## 部署测试流程

### 前提条件

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 必须，Spring Boot 3.x 要求 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 数据库 |

### 步骤

```bash
# 1. 创建 MySQL 数据库
mysql -u root -p
> CREATE DATABASE agent_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 启动后端
cd agent-server

# Windows
set DB_USERNAME=root
set DB_PASSWORD=your_password
mvn spring-boot:run

# Linux/Mac
DB_USERNAME=root DB_PASSWORD=your_password mvn spring-boot:run

# 3. 验证启动成功
# 控制台看到 "Default admin user created: admin / admin123"
# 访问 http://localhost:8080/api/auth/login 返回登录页

# 4. 生产打包
mvn clean package
java -jar target/agent-server-2.0.0.jar
```

### 首次使用验证

1. 启动后端 → 确认控制台输出 `Default admin user created`
2. 用 `admin` / `admin123` 登录前端
3. 进入「模型配置」→ 选择预设模板 → 填入 API Key → 保存
4. 返回对话页面 → 输入消息 → 验证流式输出

---

## API 接口汇总

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | - | 用户注册 |
| POST | `/api/auth/login` | - | 用户登录 |
| POST | `/api/auth/refresh` | - | 刷新 Token |
| GET | `/api/auth/me` | ✓ | 获取当前用户信息 |
| POST | `/api/chat/stream` | ✓ | SSE 流式对话 |
| GET | `/api/models` | ✓ | 模型配置列表 |
| GET | `/api/models/presets` | ✓ | 模型预设模板 |
| POST | `/api/models` | ✓ | 创建模型配置 |
| PUT | `/api/models/{id}` | ✓ | 更新模型配置 |
| DELETE | `/api/models/{id}` | ✓ | 删除模型配置 |
| GET | `/api/conversations` | ✓ | 会话列表（按用户隔离） |
| GET | `/api/conversations/{id}` | ✓ | 获取会话详情 |
| GET | `/api/conversations/{id}/messages` | ✓ | 获取会话消息 |
| PUT | `/api/conversations/{id}` | ✓ | 更新会话标题 |
| DELETE | `/api/conversations/{id}` | ✓ | 删除会话 |
| GET | `/api/skills` | ✓ | 内置技能列表 |
| PUT | `/api/skills/{id}/toggle` | ✓ | 启用/禁用技能 |
| GET | `/api/http-tools` | ✓ | HTTP 工具列表 |
| POST | `/api/http-tools` | ✓ | 创建 HTTP 工具 |
| PUT | `/api/http-tools/{id}` | ✓ | 更新 HTTP 工具 |
| PUT | `/api/http-tools/{id}/toggle` | ✓ | 启用/禁用 HTTP 工具 |
| DELETE | `/api/http-tools/{id}` | ✓ | 删除 HTTP 工具 |
| GET | `/api/plugins` | ✓ | 插件列表 |
| POST | `/api/plugins` | ✓ | 上传插件 (multipart) |
| PUT | `/api/plugins/{id}` | ✓ | 更新插件 |
| PUT | `/api/plugins/{id}/toggle` | ✓ | 启用/禁用插件 |
| DELETE | `/api/plugins/{id}` | ✓ | 删除插件 |
| GET | `/api/knowledge-bases` | ✓ | 知识库列表（按用户隔离） |
| POST | `/api/knowledge-bases` | ✓ | 创建知识库 |
| DELETE | `/api/knowledge-bases/{id}` | ✓ | 删除知识库 |
| GET | `/api/knowledge-bases/{id}/documents` | ✓ | 文档列表 |
| POST | `/api/knowledge-bases/{id}/documents` | ✓ | 上传文档 (multipart) |
| DELETE | `/api/knowledge-bases/{kbId}/documents/{docId}` | ✓ | 删除文档 |

---

## 注意事项

### JWT 密钥安全
- `app.jwt.secret` 默认值仅用于开发，**生产环境必须修改**为至少 32 字符的随机字符串
- 修改密钥后所有已签发的 Token 立即失效

### 默认管理员
- 首次启动自动创建 `admin` / `admin123`
- **生产环境务必修改密码**

### Milvus 向量检索（可选）
开发阶段默认禁用。启用知识库语义检索：
1. 启动 Milvus: `docker run -d --name milvus -p 19530:19530 milvusdb/milvus:latest standalone`
2. 修改 `application.yml`: `milvus.enabled: true`
3. 移除 `AgentServerApplication.java` 中 `spring.autoconfigure.exclude` 的 Milvus 排除配置

### Elasticsearch 全文检索（可选）
与 Milvus 配合实现混合检索：
1. 启动 ES: `docker run -d --name es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.12.0`
2. 修改 `application.yml`: `app.search.es-enabled: true`

### 端口冲突
- 后端默认 `8080`，修改 `server.port` 后需同步更新前端 `vite.config.ts` 的代理目标

### Tool 工具安全
- `app.work-dir` 控制 FileTools/ShellTools 的操作根目录
- Shell 命令在 `work-dir` 下执行，生产环境需限制为安全目录
