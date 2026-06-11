# Agent Web — 前端

基于 React + TypeScript + Vite 的 AI 对话平台前端 SPA，提供类 DeepSeek/豆包的对话交互体验，支持流式输出、多模型切换、技能管理、知识库等能力。

---

## 技术架构

| 层 | 技术 | 版本 | 用途 |
|---|---|---|---|
| UI 框架 | React | 18 | 组件化渲染 |
| 类型系统 | TypeScript | 5.6 | 全量类型定义 |
| 构建工具 | Vite | 6 | 极速 HMR + 生产构建 |
| UI 组件库 | Ant Design | 5.22 | 完整组件体系（表单/Modal/Drawer/Drawer 等） |
| 状态管理 | Zustand | 5 | 轻量级全局状态（替代 Redux） |
| Markdown | react-markdown + remark-gfm + rehype-highlight | - | 消息渲染 + 代码高亮 |
| 路由 | react-router-dom | 6.28 | 客户端路由 |
| 图标 | @ant-design/icons | 5.5 | 图标库 |

---

## 文件结构

```
agent-web/
├── package.json                  # 依赖与脚本
├── vite.config.ts                # Vite 配置（开发代理 + 端口）
├── tsconfig.json                 # TypeScript 配置
├── index.html                    # HTML 入口
│
└── src/
    ├── main.tsx                  # React 挂载入口
    ├── App.tsx                   # 根组件（认证路由：已登录→AppLayout / 未登录→LoginPage）
    ├── index.css                 # 全局样式
    │
    ├── types/index.ts            # TypeScript 类型定义（Auth/Chat/Model/Skill/HttpTool/Plugin 等）
    │
    ├── api/index.ts              # 统一 API 层
    │   ├── fetchJSON()           # REST 封装（自动注入 JWT Header、401 自动跳转登录）
    │   ├── authApi               # 登录/注册/刷新 Token
    │   ├── modelApi              # 模型配置 CRUD + 预设模板
    │   ├── conversationApi       # 会话列表/详情/消息/删除
    │   ├── skillApi              # 内置技能列表 + 启用/禁用
    │   ├── httpToolApi           # HTTP API 工具 CRUD
    │   ├── pluginApi             # jar 插件 CRUD
    │   ├── knowledgeApi          # 知识库 + 文档管理
    │   └── streamChat()          # SSE 流式对话（EventSource 解析）
    │
    ├── stores/                   # Zustand 状态管理
    │   ├── authStore.ts          # 认证状态（token/isAuthenticated/login/logout/register）
    │   └── chatStore.ts          # 对话状态（会话/消息/模型/Skill/流式发送/中断）
    │
    └── components/
        ├── auth/
        │   └── LoginPage.tsx     # 登录/注册页面（双 Tab，Form 验证，居中卡片布局）
        │
        ├── layout/
        │   ├── AppLayout.tsx     # 主布局（Ant Layout 左侧 Sidebar + 右侧内容区）
        │   └── Sidebar.tsx       # 侧边栏：
        │                           ① 新建对话按钮
        │                           ② 会话列表（切换/删除）
        │                           ③ 功能入口（模型配置/技能管理/知识库 Drawer）
        │                           ④ 用户头像 + 退出登录菜单
        │
        ├── chat/
        │   ├── ChatView.tsx      # 对话主视图（消息列表 + 自动滚动到底部）
        │   ├── ChatInput.tsx     # 输入框（Enter 发送、Shift+Enter 换行、流式时可中断）
        │   └── MessageBubble.tsx # 消息气泡（用户/AI 双样式、react-markdown 渲染 + 代码高亮）
        │
        ├── model/
        │   └── ModelConfigPanel.tsx  # 模型配置面板（Drawer 抽屉）：
        │                               ① 已有配置列表（编辑/删除/设为默认）
        │                               ② 从预设模板快速创建
        │                               ③ 手动填写 baseUrl/apiKey/modelName/temperature 等
        │
        ├── skill/
        │   └── SkillPanel.tsx    # 技能管理面板（Drawer 抽屉，三 Tab）：
        │                           ① 内置工具：列表 + Switch 开关
        │                           ② HTTP API：CRUD 表单（URL/Method/Header/Body 模板/参数 Schema）
        │                           ③ jar 插件：上传 jar 文件 + 指定 mainClass
        │
        └── knowledge/
            └── KnowledgePanel.tsx # 知识库管理面板（Drawer 抽屉）：
                                    ① 知识库列表（创建/删除）
                                    ② 文档列表（上传/删除/状态展示）
```

---

## 核心功能

### 1. 登录注册

- **LoginPage**: 登录 / 注册 双 Tab 切换，Ant Design Form 表单验证
- **Token 管理**: 登录成功后存储 `accessToken` + `refreshToken` 到 localStorage
- **自动跳转**: API 层检测到 401 响应时自动清除 Token 并跳转到登录页
- **authStore**: Zustand 管理认证状态，App.tsx 根据 `isAuthenticated` 条件渲染

### 2. 流式对话

- **SSE 解析**: `streamChat()` 使用 `fetch()` + `ReadableStream` 逐行解析 SSE 事件
- **事件类型处理**:
  - `content`: 增量追加到 assistant 消息
  - `done`: 标记流式完成，更新 conversationId，刷新会话列表
  - `error`: 在消息末尾追加错误提示
- **AbortController**: 用户可随时中断正在进行的流式输出
- **Markdown 渲染**: MessageBubble 使用 react-markdown + remark-gfm + rehype-highlight 渲染

### 3. 多模型切换

- **ModelConfigPanel**: 支持从 6 种预设模板（DeepSeek/GPT-4o/Claude/通义千问/GLM-4/Moonshot）一键创建
- **配置字段**: 名称、baseUrl、apiKey、modelName、temperature、maxTokens、isDefault
- **对话选择**: Sidebar 中当前模型下拉切换，chatStore 维护 `currentModelId`

### 4. 技能管理 (三 Tab)

- **内置工具**: 展示 8 个后端 @Tool（file_read/code_search/shell_exec 等），Switch 控制启用
- **HTTP API**: 表单配置外部 API 工具（URL / Method / Header / Body 模板 / 参数 JSON Schema）
- **jar 插件**: 上传 jar 文件 + 指定 mainClass，后端动态加载执行

### 5. 知识库管理

- **知识库列表**: 创建/删除知识库（按用户隔离）
- **文档管理**: 上传文档（支持任意格式），后端 Tika 解析后分块存储
- **检索 (待启用)**: Milvus + ES 混合检索 + RRF 融合排序

---

## 配置说明

### vite.config.ts

```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,           // 开发服务器端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true,
      },
    },
  },
})
```

> **注意**: 后端地址修改后需同步更新 `proxy.target`

### 主题配置

App.tsx 中通过 Ant Design ConfigProvider 统一设置：
- `locale: zhCN` — 中文语言包
- `colorPrimary: '#6366f1'` — 主题色（Indigo-500）

### API 基础地址

所有 API 请求通过 `api/index.ts` 的 `fetchJSON()` 发送，开发环境走 Vite 代理，生产环境需配置 Nginx 反向代理。

---

## 部署测试流程

### 前提条件

| 依赖 | 版本 | 说明 |
|------|------|------|
| Node.js | 18+ | 必须，Vite 6 要求 |
| npm | 9+ | 随 Node.js 安装 |

### 步骤

```bash
# 1. 安装依赖（首次或依赖变更时）
cd agent-web
npm install

# 2. 开发模式（HMR 热更新）
npm run dev
# → http://localhost:5173

# 3. 生产构建
npm run build
# → 产物在 dist/ 目录，可部署到 Nginx/CDN

# 4. 预览生产构建
npm run preview
```

### 完整启动顺序

```
1. 启动 MySQL（确保 agent_platform 数据库已创建）
2. 启动后端: cd agent-server && mvn spring-boot:run
3. 启动前端: cd agent-web && npm run dev
4. 浏览器打开 http://localhost:5173
5. 使用 admin / admin123 登录
```

---

## 注意事项

### 后端必须先启动

前端所有 API 请求通过 Vite 代理转发到 `localhost:8080`，后端未启动时会报网络错误。

### Token 过期处理

- `accessToken` 有效期 24 小时，过期后 API 返回 401
- `fetchJSON()` 自动检测 401，清除 localStorage 中的 Token，页面自动跳转到登录页
- `refreshToken` 有效期 7 天，可用于无感刷新（当前版本未实现自动刷新，需手动重新登录）

### PowerShell 执行策略

Windows PowerShell 可能阻止 npm 脚本执行，需修改执行策略：
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 生产部署 (Nginx)

生产环境需配置 Nginx 反向代理：
```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # SSE 流式输出支持
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}
```

### 端口冲突

- 前端开发服务器默认 `5173`，可在 `vite.config.ts` 中修改
- 确保后端 `app.cors.allowed-origins` 包含前端开发地址
