# AGENTS.md

Compact guidance for agents working in this repo.

## Project

Goat Cloud — Java 17 / Spring Boot 3.3.5 enterprise AI platform. Monolith via `goat-boot`, Vue 3 frontend. Early-stage: no tests, no CI, some modules are placeholders.

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
- `CLAUDE.md` says 7 Flyway migrations → actually 13 (V1–V13)

## Directory Layout

```
goat-cloud/
├── backend/
│   ├── goat-boot/          # Launcher + Flyway migrations (13 versions)
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

## Conventions

- No tests exist — `mvn -DskipTests compile` is the standard build.
- No CI/CD configured yet.
- Migrations are in `backend/goat-boot/src/main/resources/db/migration/`.
- Swagger UI at `http://localhost:8080/swagger-ui.html`.
- Default credentials: `admin` / `Admin@123456`.
