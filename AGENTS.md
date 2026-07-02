# AGENTS.md

Compact guidance for agents working in this repo.

## Project

Goat Cloud — Java 17 / Spring Boot 3.3.5 enterprise AI platform. Monolith via `goat-boot`, Vue 3 frontend. No tests, no CI, some modules are placeholders.

## Build & Run

```bash
# Backend build (from project root — tests don't exist)
mvn -q -DskipTests compile

# Run backend (from project root)
mvn spring-boot:run -pl backend/goat-boot

# Frontend dev server
cd frontend/goat-ui && npm install && npm run dev

# Frontend lint
cd frontend/goat-ui && npm run lint

# Frontend type-check
cd frontend/goat-ui && npx vue-tsc --noEmit
```

**Infrastructure** (in `backend/`):
```bash
cd backend && docker compose up -d    # PostgreSQL :5432 + Redis :6379
```

## Critical Path Corrections

The `CLAUDE.md` file has stale paths — do NOT follow them:
- `CLAUDE.md` says `cd goat-ui` → actual path is `frontend/goat-ui`
- `CLAUDE.md` says `-pl goat-boot` → actual is `-pl backend/goat-boot`
- `CLAUDE.md` says `.\scripts\` for smoke tests → actual is `.\backend\scripts\`
- `CLAUDE.md` says 7 Flyway migrations → actually 17 (V1–V17)

## Directory Layout

```
goat-cloud/
├── backend/
│   ├── goat-boot/          # Launcher + Flyway migrations (17 versions)
│   ├── goat-auth/          # JWT login, token handling
│   ├── goat-common/        # 12 shared sub-modules (core, security, mybatis, log, ...)
│   ├── goat-upms/
│   │   ├── goat-upms-api/  # DTOs, request/response models
│   │   └── goat-upms-biz/  # Business logic (27 controllers)
│   ├── goat-gateway/       # Placeholder (future microservice)
│   ├── goat-register/      # Placeholder (future microservice)
│   ├── goat-visual/        # Placeholder (monitor, codegen, quartz)
│   ├── docker-compose.yml  # PostgreSQL + Redis
│   └── scripts/            # PowerShell smoke tests
└── frontend/
    └── goat-ui/            # Vue 3 + Vite + Element Plus
```

## Key Patterns

- **Auth**: JWT access token (30 min) + refresh token (7 days) in Redis, single session per user.
- **ORM**: MyBatis-Plus with underscore→camel mapping, logical delete (`deleted` 1/0).
- **Mappers**: System mappers scanned in `GoatCloudApplication`, AI mappers via `AiMapperScanConfig`.
- **Controllers**: AI CRUD controllers extend `BaseAiCrudController`.
- **API prefix**: All routes under `/api/`; Vite proxies `/api` → `localhost:8080`.
- **Error codes**: `ApiResponse` uses code 0=success, 4010=unauthorized, 4011=refresh expired, 4030=forbidden, 4001=validation, 5000=server error.
- **Frontend token refresh**: `http.ts` queues concurrent 401s and replays after refresh.
- **Frontend routing**: Dynamic routes resolved via `import.meta.glob`.
- **Flyway**: All migrations idempotent (`ON CONFLICT DO NOTHING`).
- **Model Router**: `AiModelRouterImpl` handles load balancing, auto-fallback, health monitoring across providers.
- **Request Logging**: `AiRequestLogService` tracks every AI call (tokens, latency, status) in `ai_request_log` table.
- **Billing**: `AiBillingService` calculates per-call cost with multi-model pricing, budget alerts at 1000/5000 CNY.
- **Content Safety**: `AiContentSafetyService` filters input/output for sensitive words and PII (phone, email, ID card, bank card).
- **Document Processing**: `AiChunkingService` supports 4 strategies (fixed-size, sentence, paragraph, semantic).
- **Dashboard**: Real-time stats from `AiRequestLogService` + `AiBillingService` (requests, tokens, cost, success rate, latency).

## Backend Profiles

| Profile | DB | Redis |
|---------|-----|-------|
| `dev` (default) | `localhost:5432/goat_cloud` | `localhost:6379/db0` |
| `test` | `localhost:5432/goat_cloud_test` | `localhost:6379/db1` |
| `prod` | `postgres:5432/goat_cloud` | `redis:6379/db0` |

## Smoke Tests

```powershell
# From backend/scripts/
powershell -ExecutionPolicy Bypass -File .\backend\scripts\api-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\backend\scripts\system-crud-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\backend\scripts\ai-crud-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\backend\scripts\ai-runtime-smoke.ps1
node .\backend\scripts\frontend-smoke.mjs
```

## Frontend

- Path alias: `@` → `./src`
- Lint: `eslint . --ext .ts,.vue`
- Build includes type-check: `vue-tsc --noEmit && vite build`
- Root `package.json` is only for Playwright (smoke test), not the app

## AI Platform Architecture

```
AI Module (module/ai/)
├── controller/
│   ├── BaseAiCrudController    # Generic CRUD (list/page/detail/save/delete)
│   ├── AiChatController        # Chat + conversation management
│   ├── AiDashboardController   # Dashboard stats (real-time from request_log + billing)
│   ├── AiKnowledgeBaseController  # Knowledge base + retrieval test
│   ├── AiModelConfigController # Model config + connectivity test + usage stats
│   └── AiAgentController       # Agent management
├── service/
│   ├── AiModelRouter           # Interface for model routing
│   ├── AiBillingService        # Cost calculation, budget alerts
│   ├── AiContentSafetyService  # Input/output filtering, PII desensitization
│   ├── AiRequestLogService     # Request tracking, statistics
│   ├── AiModelTestService      # Model connectivity test, usage stats
│   ├── AiConversationService   # Conversation CRUD + history
│   ├── AiDocumentService        # Document upload, parse, chunk, vectorize
│   └── impl/
│       ├── AiModelRouterImpl   # Load balancing, fallback, health monitoring
│       ├── AiChunkingServiceImpl  # 4 chunking strategies
│       └── AiDocumentServiceImpl  # Full document processing pipeline
├── entity/
│   ├── AiModelConfig           # Model provider/endpoint/pricing
│   ├── AiKnowledgeBase         # Knowledge base metadata
│   ├── AiDocument              # Document with parse/chunk status tracking
│   ├── AiDocumentChunk         # Text chunks with token count
│   ├── AiConversation          # Chat conversations
│   ├── AiConversationRecord    # Messages with model_id/token stats
│   ├── AiAgent                 # Agent definitions
│   ├── AiBillingRecord         # Per-call billing records
│   └── AiRequestLog            # Request-level tracking (tokens, latency, status)
└── runtime/
    ├── AiRuntimeService        # Orchestrator for chat/RAG/agent/workflow
    ├── AiChatService           # Chat execution
    ├── AiRagSearchService      # RAG retrieval
    ├── AiAgentService          # Agent execution
    ├── AiWorkflowService       # Workflow execution
    └── AiChatBiService         # BI chat
```

## Database Migrations

| Version | Description |
|---------|-------------|
| V1–V13 | System tables (users, roles, menus, depts, etc.) |
| V14 | AI tables (models, knowledge bases, documents, agents) |
| V15 | AI conversation and billing tables |
| V16 | Add model stats to ai_conversation_record (model_id, prompt_tokens, completion_tokens) |
| V17 | Create ai_request_log table with indexes |

## Conventions

- No tests exist — `mvn -DskipTests compile` is the standard build.
- No CI/CD configured yet.
- Migrations are in `backend/goat-boot/src/main/resources/db/migration/`.
- Swagger UI at `http://localhost:8080/swagger-ui.html`.
- Default credentials: `admin` / `Admin@123456`.
