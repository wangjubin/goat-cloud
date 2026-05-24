# Local Setup

## PostgreSQL

```sql
create database goat_cloud;
```

默认连接：

- host: `localhost`
- port: `5432`
- username: `postgres`
- password: `postgres`

## Redis

默认连接：

- host: `localhost`
- port: `6379`
- database: `0`

## Backend

```bash
cd D:\workspace\goat-cloud
mvn -q -DskipTests compile
mvn spring-boot:run -pl goat-boot
```

## Frontend

```bash
cd D:\workspace\goat-cloud\goat-ui
npm install
npm run dev
```

## API Smoke Test

```powershell
cd D:\workspace\goat-cloud
powershell -ExecutionPolicy Bypass -File .\scripts\api-smoke.ps1
```

可选参数：

- `-BaseUrl http://localhost:8080`
- `-Username admin`
- `-Password Admin@123456`

## Frontend Smoke Test

```powershell
cd D:\workspace\goat-cloud
node .\scripts\frontend-smoke.mjs
```

可选环境变量：

- `FRONTEND_BASE_URL=http://localhost:5173`
- `FRONTEND_SMOKE_DEBUG_PORT=9222`
- `FRONTEND_SMOKE_USERNAME=admin`
- `FRONTEND_SMOKE_PASSWORD=Admin@123456`
- `FRONTEND_SMOKE_ROUTES=/dashboard,/system/users`
- `FRONTEND_SMOKE_SKIP_LOGIN=1`
- `FRONTEND_SMOKE_EXPECT_LOGIN_REDIRECT=1`

仅检查未登录访问根路径是否正常跳转登录页：

```powershell
cd D:\workspace\goat-cloud
$env:FRONTEND_SMOKE_SKIP_LOGIN='1'
$env:FRONTEND_SMOKE_EXPECT_LOGIN_REDIRECT='1'
$env:FRONTEND_SMOKE_ROUTES='/'
node .\scripts\frontend-smoke.mjs
```

## Default Seed

- username: `admin`
- password: `Admin@123456`
- role: `SYSTEM_ADMIN`
- dept: `Goat Cloud`
