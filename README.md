# Goat Cloud

**企业级 AI 智能云平台** —— 一站式集成 RBAC 权限管理、RAG 知识检索、ChatBI 智能问数、Agent 智能体编排、StateGraph 工作流引擎与 MCP 工具协议的完整解决方案。

> **设计哲学**：把大模型、向量检索、NL2SQL、工作流这些 AI 能力作为平台一等公民,而非外挂——传统权限体系与 AI 运行时在数据层、会话层、审计层深度整合。

---

## 目录

- [核心特性](#核心特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [AI 平台管理](#ai-平台管理)
- [ChatBI 智能问数](#chatbi-智能问数)
- [StateGraph 工作流](#stategraph-工作流)
- [数据库迁移](#数据库迁移)
- [冒烟测试](#冒烟测试)
- [License](#license)

---

## 核心特性

### 安全与权限
- **JWT + Refresh Token**(30 min / 7 天),Redis 单会话控制
- **RBAC 完整体系**:用户/角色/菜单/部门 + 数据级权限切面
- **SQL 注入阻断**:AI 生成的 SQL 强制 SELECT/WITH 只读,关键字过滤
- **API Key 加密存储**(AES),健康探测与速率限制

### AI 运行时
- **多模型适配**:OpenAI / DeepSeek / Anthropic / 通义 / 智谱 / Ollama 等,统一接口
- **RAG 知识检索**:文档上传 → 解析(PDF/DOCX/MD/HTML)→ 语义切片 → 向量化 → 混合检索
- **ChatBI 智能问数**:自然语言 → 意图识别 → Schema 召回 → NL2SQL → 人工确认 → 报告
- **StateGraph 工作流**:SEQUENTIAL / DAG / PARALLEL 三种模式,11 种节点执行器
- **MCP 协议**:HTTP/STDIO/SSE 三种传输,工具发现 + 健康检查
- **可视化编辑器**:SVG 拖拽编排工作流节点与条件边

### 可观测
- **执行轨迹追踪**:Plan/Traces 全程记录,断点恢复
- **Token 计费**:逐请求用量与成本,多维统计
- **MCP 连接日志**:状态/耗时/错误全量审计

### 人机协同
- **Human-in-the-Loop**:SQL 执行前人工确认(批准/修改/拒绝/出图)
- **会话持久化**:对话历史 + 思考过程完整保留
- **断点续传**:中断的 StateGraph 会话可从任意节点恢复

---

## 系统架构

```
                    ┌──────────────────────────────────────┐
                    │           Frontend (Vue 3)            │
                    │  Element Plus + ECharts + Pinia       │
                    │  Vite Proxy /api → :8080              │
                    └──────────────┬───────────────────────┘
                                   │
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
                    │  │  ┌────────┐ ┌────────┐ ┌──────┐ │  │
                    │  │  │  RAG   │ │ ChatBI │ │Agent │ │  │
                    │  │  │检索引擎 │ │(NL2SQL)│ │运行台│ │  │
                    │  │  └────────┘ └────────┘ └──────┘ │  │
                    │  │  ┌────────────────────────────┐ │  │
                    │  │  │  StateGraph 工作流引擎      │ │  │
                    │  │  └────────────────────────────┘ │  │
                    │  │  ┌────────┐ ┌────────────────┐  │  │
                    │  │  │  MCP   │ │ Prompt/Billing │  │  │
                    │  │  │ Client │ │ /Chunk/Vector  │  │  │
                    │  │  └────────┘ └────────────────┘  │  │
                    │  └─────────────────────────────────┘  │
                    └──────────────┬───────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────┐
                    │  PostgreSQL 16 + pgvector │ Redis 7.4 │
                    │  (向量索引/业务数据)         │ (会话/限流)│
                    └──────────────────────────────────────┘
```

---

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 后端语言 | Java | 17 |
| 后端框架 | Spring Boot / Spring Cloud | 3.3.5 / 2023.0.3 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | PostgreSQL + pgvector | 16 |
| 缓存 | Redis | 7.4 |
| 认证 | JWT (jjwt) | 0.12.6 |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 构建 | Maven + Lombok + MapStruct | — |
| 前端语言 | TypeScript | 5.6 |
| 前端框架 | Vue 3 / Vite | 3.5 / 5.4 |
| UI 库 | Element Plus | 2.8 |
| 状态管理 | Pinia | 2.2 |
| 图表 | ECharts | 6.1 |
| HTTP | Axios | 1.7 |

---

## 快速开始

### 环境依赖

- **JDK** 17+
- **Node.js** 18+
- **Docker / Docker Compose**(用于 PostgreSQL 与 Redis)
- **Maven** 3.8+

### 1. 启动基础设施

```bash
cd backend
docker compose up -d
```

- PostgreSQL:`localhost:5432`,数据库 `goat_cloud`,用户/密码 `postgres / postgres`
- Redis:`localhost:6379`,db0

### 2. 启动后端

```bash
# 从项目根目录编译
mvn -q -DskipTests compile

# 启动 Spring Boot 单体(Flyway 自动建表与初始数据)
mvn spring-boot:run -pl backend/goat-boot
```

- API 地址:`http://localhost:8080`
- Swagger 文档:`http://localhost:8080/swagger-ui.html`
- 健康检查:`http://localhost:8080/actuator/health`

### 3. 启动前端

```bash
cd frontend/goat-ui
npm install
npm run dev
```

- 前端地址:`http://localhost:5173`
- Vite 自动代理 `/api` → `http://localhost:8080`

### 4. 类型检查与构建

```bash
cd frontend/goat-ui

# TypeScript 类型检查
npx vue-tsc --noEmit

# 生产构建
npm run build

# 代码风格
npm run lint
```

### 默认账号

| 用户名 | 密码 |
|--------|------|
| `admin` | `Admin@123456` |

---

## 项目结构

```
goat-cloud/
├── backend/                          # 后端 (Java 17 / Spring Boot 3)
│   ├── pom.xml                       # 父 POM,聚合所有模块
│   ├── docker-compose.yml            # PostgreSQL + Redis 一键启动
│   ├── goat-boot/                    # 单体启动器 + Flyway 迁移 (15 个版本)
│   ├── goat-auth/                    # 认证模块 (JWT / Session)
│   ├── goat-common/                  # 公共库 (12 子模块)
│   │   ├── goat-common-core/         #   通用响应 / 基础实体 / 异常
│   │   ├── goat-common-security/     #   JWT 过滤器 / Session / 权限切面
│   │   ├── goat-common-mybatis/      #   MyBatis-Plus 配置 / 审计填充
│   │   ├── goat-common-log/          #   全局异常处理
│   │   ├── goat-common-swagger/      #   OpenAPI 文档配置
│   │   └── ...                       #   datasource / feign / oss / seata / websocket / xss / bom
│   ├── goat-upms/                    # 用户权限 + AI 业务模块 (33 个 Controller)
│   │   ├── goat-upms-api/            #   DTO / 请求响应模型
│   │   └── goat-upms-biz/            #   完整服务层 + 控制器
│   ├── goat-gateway/                 # API 网关占位 (微服务拆分预留)
│   ├── goat-register/                # 注册中心占位 (微服务拆分预留)
│   ├── goat-visual/                  # 监控 / 代码生成 / 定时任务占位
│   ├── docs/                         # 部署与本地搭建文档
│   └── scripts/                      # 冒烟测试脚本 (PowerShell)
│
├── frontend/                         # 前端 (Vue 3 / TypeScript)
│   └── goat-ui/
│       ├── src/views/system/         # 系统管理 (用户 / 角色 / 菜单 / 部门)
│       ├── src/views/ai/             # AI 平台管理 (11 个管理页 + ChatBI 工作流)
│       │   ├── model/                #   模型管理
│       │   ├── vector/               #   向量配置
│       │   ├── knowledge/            #   知识库
│       │   ├── document/             #   文档管理
│       │   ├── chunk/                #   切片管理
│       │   ├── prompt/               #   提示词模板
│       │   ├── mcp/                  #   MCP 服务
│       │   ├── api-skill/            #   API 工具
│       │   ├── workflow/             #   工作流编排
│       │   ├── agent/                #   智能体
│       │   ├── billing/              #   账单统计
│       │   └── ask/                  #   ChatBI 智能问数 (对话 + 可视化编排)
│       ├── src/api/                  # API 调用层 (含 SSE 流式通信)
│       ├── src/stores/               # Pinia 状态 (auth / user / permission)
│       └── src/components/           # 通用组件
│
├── CLAUDE.md                         # Claude Code 项目指南
├── AGENTS.md                         # Agent 协作约定
└── README.md
```

---

## AI 平台管理

所有 11 个 AI 管理页面均采用统一的**卡片式视觉语言**:`头部 + 4 维统计 + 筛选器 + 卡片网格 + 编辑抽屉`,支持深浅色主题。

| 页面 | 核心能力 |
|------|---------|
| **模型管理** | 多供应商(OpenAI / DeepSeek / Anthropic / 通义 / 智谱 / Ollama),Provider 预设自动填充端点,API Key 加密 + 显隐切换,能力标签多选,默认模型星标 |
| **向量配置** | 多引擎(POSTGRESQL / ELASTICSEARCH / MILVUS / 内存索引),维度/距离/索引类型三维配置 |
| **知识库** | 文档/切片/维度三栏统计,向量配置下拉,嵌入模型 + 检索配置 JSON |
| **文档管理** | 拖拽上传,文件类型渐变图标,实时解析/切片状态,错误信息展示 |
| **切片管理** | 按文档分组,知识库→文档级联下拉,向量化/Token 用量统计 |
| **提示词模板** | 场景化分类(对话 / RAG / Agent / 代码 / 翻译 / 摘要),系统+用户双提示词,变量占位 |
| **MCP 服务** | stdio / SSE / HTTP 三种传输,健康状态徽标(UP / DOWN / UNKNOWN),能力清单 |
| **API 工具** | HTTP 方法彩色徽标(GET/POST/PUT/DELETE/PATCH),Bearer/API Key/Basic 鉴权 |
| **工作流** | 触发方式(手动/定时/事件)渐变图标,节点计数,SVG 可视化编辑器(单独页) |
| **智能体** | 模型 + 提示词 + 工具 + 知识库绑定可视化,运行抽屉(RAG / ChatBI 开关) |
| **账单统计** | 累计费用 / Token / 调用次数 / 成功率,厂商费用 TOP 榜,Token 消耗条形图,CSV 导出 |

---

## ChatBI 智能问数

```
用户提问: "最近7天的注册用户数"
    │
    ▼
意图识别 ──→ Schema召回 ──→ NL2SQL生成 ──→ 人工确认 ──→ SQL执行 ──→ 报告生成
    │           │              │              │            │            │
 DATA_QUERY   表结构缓存    SELECT COUNT(*)   确认/修改   结果集       ECharts
 TREND...     24h TTL       安全过滤(只读)    中断/恢复   100行限制    柱/线/饼
```

| 能力 | 描述 |
|------|------|
| 意图识别 | 关键词 + 配置规则匹配(数据查询 / 趋势分析 / 数据对比 / 报表生成 / 根因分析) |
| Schema 召回 | 24h 缓存,按相关性评分召回表结构 |
| NL2SQL | LLM 驱动,强制 SELECT/WITH 只读,SQL 注入阻断 |
| 人工确认 | HITL 中断/恢复,批准 / 修改 / 拒绝 / 批准并出图 |
| SQL 执行 | 安全沙箱,100 行限制,执行日志全量 |
| 报告生成 | 模板匹配或 LLM 生成 ECharts 配置,实时渲染 |
| 数据源 | 多数据源导入 + 表结构自动同步 + 业务术语表 |

---

## StateGraph 工作流

插件化的图执行引擎,支持三种运行模式:

| 模式 | 描述 |
|------|------|
| `SEQUENTIAL` | 节点按排序顺序执行,遇中断/失败停止 |
| `DAG` | 拓扑排序,支持条件边路由(`==`, `!=`)与 AND/OR/XOR 网关 |
| `PARALLEL` | 所有节点并行执行,结果合并 |

**内置 11 种节点执行器:**

| 节点类型 | 用途 | 可中断 |
|---------|------|--------|
| `START` / `END` | 图入口/出口 | 否 |
| `GATEWAY` (AND/OR/XOR) | 条件路由 | 否 |
| `INTENT_RECOGNITION` | 意图识别 | 否 |
| `SCHEMA_RECALL` | Schema 召回 | 否 |
| `NL2SQL` | 自然语言转 SQL | 否 |
| `SQL_EXECUTION` | SQL 执行 | 否 |
| `HUMAN_FEEDBACK` | 人工确认 | **是** |
| `MCP_TOOL` | 调用外部 MCP 工具 | 否 |
| `PYTHON_EXECUTION` | Python 代码执行 | 否 |
| `REPORT_GENERATION` | 图表报告生成 | 否 |
| `LLM_CALL` | 通用大模型调用 | 否 |

**SSE 实时流式输出:** 15s 心跳 / 120s 超时 / 客户端断开检测,事件类型覆盖 `node_start` / `node_complete` / `node_error` / `interrupt` / `complete` / `heartbeat`。

**可视化编辑器:** SVG 拖拽式节点编排,支持条件边、DAG 拓扑、节点配置面板。

---

## 数据库迁移

Flyway 自动管理,共 15 个版本:

| 版本 | 内容 |
|------|------|
| `V1` | 系统基础表(用户/角色/菜单/部门/日志) |
| `V2` | 种子数据(管理员账号、菜单权限) |
| `V3` | AI 基础表(15 表 + 种子) |
| `V4` | 系统国际化(i18n) |
| `V5` | 中文种子数据规范化 |
| `V6` | PostgreSQL 序列同步 |
| `V7` | 模型扩展(API Key 加密 / 速率限制 / 健康状态) |
| `V8` | ChatBI StateGraph 表(12 表) |
| `V9` | ChatBI 菜单与种子数据 |
| `V10` | DAG 编排支持(条件边 / 图类型 / 子图) |
| `V11` | ChatBI 数据源模型与表结构导入 |
| `V12` | 工作流与 StateGraph 同步 |
| `V13` | pgvector 向量索引支持 |
| `V14` | AI 配置权限 |
| `V15` | AI 对话持久化 |

所有迁移使用 `ON CONFLICT DO NOTHING / DO UPDATE` 保证幂等。

---

## 冒烟测试

`backend/scripts/` 提供 4 个 PowerShell 冒烟测试脚本,覆盖接口可用性:

```powershell
# 系统管理 / 通用接口
powershell -ExecutionPolicy Bypass -File backend\scripts\api-smoke.ps1

# UPMS 系统 CRUD(用户 / 角色 / 菜单 / 部门)
powershell -ExecutionPolicy Bypass -File backend\scripts\system-crud-smoke.ps1

# AI 模块 CRUD(模型 / 知识库 / 文档 / 切片 / MCP / API / 工作流 / 智能体 / 账单等 56 个端点)
powershell -ExecutionPolicy Bypass -File backend\scripts\ai-crud-smoke.ps1

# AI 运行时(ChatBI 流式问答 / Agent 运行 / 文档上传)
powershell -ExecutionPolicy Bypass -File backend\scripts\ai-runtime-smoke.ps1
```

---

## License

Private — All rights reserved.
