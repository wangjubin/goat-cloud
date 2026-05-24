# Goat Cloud

**企业级 AI 智能云平台** — 融合 RBAC 权限管理、RAG 知识检索、ChatBI 智能问数、Agent 智能体编排与 StateGraph 工作流引擎的一站式解决方案。

---

## 设计理念

Goat Cloud 的核心设计思想是 **"AI 即基础设施"**：将大语言模型、向量检索、NL2SQL、工作流编排等 AI 能力作为一等公民融入企业平台，而非简单外挂。平台将传统权限体系与 AI 运行时深度整合，实现：

- **安全优先** — JWT + Redis 单会话 + SQL 注入防护 + 数据权限切面，AI 生成 SQL 强制只读
- **可观测** — 完整的执行轨迹追踪、Token 用量计费、MCP 连接日志
- **可扩展** — 插件化 NodeExecutor、MCP 协议接入、DAG 工作流支持条件分支
- **人机协同** — Human-in-the-Loop 中断/恢复机制，SQL 执行前人工确认

---

## 系统架构

```
                    ┌──────────────────────────────────────┐
                    │           Frontend (Vue 3)            │
                    │  Element Plus + ECharts + Pinia       │
                    └──────────────┬───────────────────────┘
                                   │  Vite Proxy /api
                    ┌──────────────▼───────────────────────┐
                    │         Backend (Spring Boot 3)       │
                    │                                       │
                    │  ┌─────────┐  ┌──────────┐           │
                    │  │  Auth   │  │   UPMS   │           │
                    │  │ (JWT)   │  │ (RBAC)   │           │
                    │  └─────────┘  └──────────┘           │
                    │                                       │
                    │  ┌─────────────────────────────────┐  │
                    │  │        AI Runtime Engine          │  │
                    │  │                                   │  │
                    │  │  ┌─────────┐  ┌───────────────┐  │  │
                    │  │  │  RAG    │  │  ChatBI       │  │  │
                    │  │  │ Search │  │  (NL2SQL)     │  │  │
                    │  │  └─────────┘  └───────────────┘  │  │
                    │  │  ┌─────────┐  ┌───────────────┐  │  │
                    │  │  │ Agent   │  │ StateGraph    │  │  │
                    │  │  │ Runtime│  │ Workflow      │  │  │
                    │  │  └─────────┘  └───────────────┘  │  │
                    │  │  ┌─────────┐  ┌───────────────┐  │  │
                    │  │  │  MCP   │  │ Report/Chart  │  │  │
                    │  │  │ Client │  │ Generation    │  │  │
                    │  │  └─────────┘  └───────────────┘  │  │
                    │  └─────────────────────────────────┘  │
                    └──────────────┬───────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────┐
                    │     PostgreSQL 16  │  Redis 7.4       │
                    │  (pgvector 扩展)   │  (会话/缓存)     │
                    └──────────────────────────────────────┘
```

---

## 核心功能

### 1. 权限管理 (UPMS)

基于 RBAC 模型的完整权限体系，支持数据级权限控制：

| 功能 | 描述 |
|------|------|
| 用户管理 | 用户 CRUD、状态启禁、密码重置、角色分配 |
| 角色管理 | 角色 CRUD、菜单权限绑定、数据范围 (ALL/CUSTOM/DEPT/SELF) |
| 菜单管理 | 目录/菜单/按钮三级树形结构，动态路由下发 |
| 部门管理 | 树形部门架构，数据权限按部门隔离 |
| 认证体系 | Access Token (30min) + Refresh Token (7天)，Redis 单会话控制 |

### 2. AI 运行时引擎

统一的 AI 中间件层，提供开箱即用的 AI 能力接入：

| 能力 | 描述 |
|------|------|
| 模型管理 | 多供应商 LLM/Embedding 模型注册，API Key 加密存储，健康探测 |
| RAG 知识检索 | 文档上传 → 解析 → 分块 → 向量化 → 关键词+向量混合检索，引用溯源 |
| Prompt 模板 | 变量化 Prompt 管理，模板渲染引擎，版本追踪 |
| Token 计费 | 逐请求 Token 用量记录，成本统计 |

### 3. ChatBI 智能问数

自然语言驱动的数据分析，从提问到可视化一步到位：

```
用户提问: "最近7天的注册用户数"
    │
    ▼
意图识别 ──→ Schema召回 ──→ NL2SQL生成 ──→ 人工确认 ──→ SQL执行 ──→ 报告生成
    │           │              │              │            │            │
 DATA_QUERY   表结构缓存    SELECT COUNT(*)   确认/修改   结果集       ECharts
 TREND...     24h TTL       安全过滤(只读)    中断/恢复   100行限制    柱/线/饼
```

| 功能 | 描述 |
|------|------|
| 意图识别 | 关键词 + 配置规则匹配 (数据查询/趋势分析/数据对比/报表生成/根因分析) |
| Schema 召回 | 24 小时缓存，按相关性评分召回表结构 |
| NL2SQL | LLM 驱动 SQL 生成，强制 SELECT/WITH 只读，注入阻断 |
| 人工确认 | HITL 中断/恢复 — 批准、修改 SQL、拒绝、批准并出图 |
| SQL 执行 | 安全沙箱执行，100 行限制，执行日志全量记录 |
| 报告生成 | 模板匹配或 LLM 生成 ECharts 配置，实时渲染 |

### 4. StateGraph 工作流引擎

插件化的图执行引擎，支持三种运行模式：

| 模式 | 描述 |
|------|------|
| SEQUENTIAL | 节点按排序顺序执行，遇到中断/失败停止 |
| DAG | 拓扑排序执行，支持条件边路由 (==, !=) 和 AND/OR/XOR 网关 |
| PARALLEL | 所有节点并行执行，结果合并 |

**内置 11 种节点执行器：**

| 节点类型 | 用途 | 可中断 |
|---------|------|--------|
| START / END | 图入口/出口 | 否 |
| GATEWAY (AND/OR/XOR) | 条件路由 | 否 |
| INTENT_RECOGNITION | 意图识别 | 否 |
| SCHEMA_RECALL | Schema 召回 | 否 |
| NL2SQL | 自然语言转 SQL | 否 |
| SQL_EXECUTION | SQL 执行 | 否 |
| HUMAN_FEEDBACK | 人工确认 | **是** |
| MCP_TOOL | 调用外部 MCP 工具 | 否 |
| PYTHON_EXECUTION | Python 代码执行 | 否 |
| REPORT_GENERATION | 图表报告生成 | 否 |

**SSE 实时流式输出：** 15s 心跳、120s 超时、客户端断开检测，事件类型覆盖 `node_start` / `node_complete` / `node_error` / `interrupt` / `complete` / `heartbeat`。

### 5. MCP 协议集成

Model Context Protocol 服务器接入，扩展 AI 能力边界：

| 功能 | 描述 |
|------|------|
| 传输方式 | HTTP / STDIO / SSE (HTTP 已实现) |
| 工具发现 | 自动获取 MCP 服务器提供的工具列表 |
| 工具调用 | JSON Schema 输入/输出，模板变量解析 (`{{var}}`) |
| 认证 | Bearer / Basic / API-Key (配置化) |
| 健康检查 | 单服务器/批量健康探测，状态自动更新 |

### 6. Agent 智能体

可定制的 AI 代理，组合模型、Prompt、工具和知识库：

- 4 阶段执行：理解 → 检索 → 工具调用 → 回答
- 绑定模型、Prompt 模板、RAG 知识库、API Skill、MCP Tool
- 执行计划 (Plan) 全程可追踪

---

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 语言 | Java / TypeScript | 17 / 5.6 |
| 框架 | Spring Boot / Vue | 3.3.5 / 3.5 |
| 云 | Spring Cloud | 2023.0.3 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | PostgreSQL + pgvector | 16 |
| 缓存 | Redis | 7.4 |
| 认证 | JWT (jjwt) | 0.12.6 |
| 文档 | SpringDoc OpenAPI | 2.6.0 |
| 构建 | Maven + Lombok + MapStruct | 1.18 / 1.5.5 |
| 前端 | Vite + Element Plus + Pinia | 5.4 / 2.8 / 2.2 |
| 图表 | ECharts | 6.1 |
| HTTP | Axios | 1.7 |

---

## 快速开始

### 环境依赖

- Java 17+
- Node.js 18+
- Docker (PostgreSQL + Redis)

### 1. 启动基础设施

```bash
cd backend
docker compose up -d
```

PostgreSQL: `localhost:5432` (数据库: `goat_cloud`)  
Redis: `localhost:6379`

### 2. 启动后端

```bash
# 编译
mvn -q -DskipTests compile

# 启动 (Flyway 自动建表和初始数据)
mvn spring-boot:run -pl backend/goat-boot
```

API 地址: `http://localhost:8080`  
Swagger 文档: `http://localhost:8080/swagger-ui.html`

### 3. 启动前端

```bash
cd frontend/goat-ui
npm install
npm run dev
```

前端地址: `http://localhost:5173`

### 默认账号

| 用户名 | 密码 |
|--------|------|
| admin | Admin@123456 |

---

## 项目结构

```
goat-cloud/
├── backend/                     # 后端 (Java / Spring Boot)
│   ├── goat-boot/               # 单体启动器 + Flyway 迁移
│   ├── goat-auth/               # 认证模块 (JWT/Session)
│   ├── goat-common/             # 公共库 (12 子模块)
│   │   ├── goat-common-core/    #   通用响应、基础实体、异常
│   │   ├── goat-common-security/#   JWT 过滤器、Session、权限切面
│   │   ├── goat-common-mybatis/ #   MyBatis-Plus 配置、审计填充
│   │   ├── goat-common-log/     #   全局异常处理
│   │   ├── goat-common-swagger/ #   OpenAPI 文档配置
│   │   └── ...                   #   datasource, feign, oss, seata, websocket, xss, bom
│   ├── goat-upms/                # 用户权限 + AI 业务模块
│   │   ├── goat-upms-api/       #   DTO / 请求响应模型
│   │   └── goat-upms-biz/       #   27 控制器 + 完整服务层
│   ├── goat-gateway/            # 网关占位 (微服务预留)
│   ├── goat-register/           # 注册中心占位 (微服务预留)
│   ├── goat-visual/             # 监控/代码生成/定时任务占位
│   ├── docker-compose.yml
│   ├── docs/
│   └── scripts/                 # API 冒烟测试脚本
│
└── frontend/                    # 前端 (Vue 3 / TypeScript)
    └── goat-ui/
        ├── src/views/system/    # 系统管理 (用户/角色/菜单/部门)
        ├── src/views/ai/        # AI 平台管理 (模型/向量/Prompt/知识库/文档/MCP/Agent/工作流)
        ├── src/views/ai/ask/    # ChatBI 智能问数 (对话 + 工作流编排)
        └── src/api/             # API 调用层 (含 SSE 流式通信)
```

---

## 数据库迁移

Flyway 自动管理，共 10 个版本：

| 版本 | 内容 |
|------|------|
| V1 | 系统基础表 (用户/角色/菜单/部门/日志) |
| V2 | 种子数据 (管理员账号、菜单权限) |
| V3 | AI 基础表 (15 表 + pgvector + 种子数据) |
| V4-V5 | 国际化与中文规范化 |
| V6 | PostgreSQL 序列同步 |
| V7 | 模型扩展 (API Key 加密、速率限制、健康状态) |
| V8 | ChatBI 工作流表 (12 表) |
| V9 | ChatBI 菜单与种子数据 (默认流程/意图/模板) |
| V10 | DAG 编排支持 (条件边、图类型、子图) |

---

## License

Private — All rights reserved.