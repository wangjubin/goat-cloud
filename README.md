# Goat Cloud

Goat Cloud 的目录结构已调整为参考 `pig / pig-ui` 的组织方式，后端按 `boot / auth / common / gateway / upms / visual` 顶层分层，前端独立为 `goat-ui`。

## Structure

```text
goat-cloud
├── goat-boot -- 单体模式启动器
├── goat-auth -- 认证与登录会话模块
├── goat-common -- 公共模块集合
│   ├── goat-common-bom
│   ├── goat-common-core
│   ├── goat-common-datasource
│   ├── goat-common-log
│   ├── goat-common-oss
│   ├── goat-common-mybatis
│   ├── goat-common-seata
│   ├── goat-common-websocket
│   ├── goat-common-security
│   ├── goat-common-swagger
│   ├── goat-common-feign
│   └── goat-common-xss
├── goat-register -- 注册中心占位
├── goat-gateway -- 网关占位
├── goat-upms
│   ├── goat-upms-api -- 公共 DTO / API 模型
│   └── goat-upms-biz -- 用户权限业务模块
├── goat-visual
│   ├── goat-monitor -- 监控占位
│   ├── goat-codegen -- 代码生成占位
│   └── goat-quartz -- 定时任务占位
├── goat-ui -- Vue 管理台
└── docs
```

## Implemented

- PostgreSQL + Flyway 初始化
- Redis 单会话控制
- Access Token + Refresh Token 认证
- 用户、角色、组织、菜单基础能力
- 动态菜单、动态路由、按钮权限
- `pig` 风格的后端模块布局骨架

## Backend Run

```bash
cd D:\workspace\goat-cloud
mvn -q -DskipTests compile
mvn spring-boot:run -pl goat-boot
```

默认接口：

- `http://localhost:8080`
- `http://localhost:8080/swagger-ui.html`

## Frontend Run

```bash
cd D:\workspace\goat-cloud\goat-ui
npm install
npm run dev
```

默认地址：

- `http://localhost:5173`

## Default Account

- username: `admin`
- password: `Admin@123456`

## Notes

- 数据权限切面和上下文已经具备，后续可继续增强为更细粒度 SQL 级过滤
- `goat-register / goat-gateway / goat-visual` 当前是按 `pig` 风格建好的占位工程，便于后续继续扩展
- 会话只存 Redis，不落 PostgreSQL
