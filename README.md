# 医院预约挂号管理系统

医院预约挂号课程项目，包含 Spring Boot 后端、Vue2 + Element UI 管理后台、微信患者端小程序与数据库脚本。

## 目录结构（整理后）

```
Hospital-Appointment-Management2/
├── backend/                       # Spring Boot 4.1.2 后端（Maven 工程根，端口 9090）
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/
│       ├── java/com/aaa/backend/
│       │   ├── admin/             # 新版管理契约 /api/admin/**（RESTful + 统一响应）
│       │   ├── appointment/       # 患者预约、病历、评价
│       │   ├── catalog/           # 公开查询：科室 / 医生 / 排班
│       │   ├── prescription/      # 处方与支付
│       │   ├── notification/      # 站内消息
│       │   ├── wallet/            # 余额与流水
│       │   ├── profile/ file/     # 个人资料、头像上传
│       │   ├── triage/            # 智能分诊（含降级规则）
│       │   ├── doctor/            # 医生工作台 /api/doctor/**
│       │   ├── controller/ service/ dao/ entity/ vo/ dto/
│       │   │                      # 旧管理接口 /admin/**（迁移兼容，删除能力在此）
│       │   ├── common/            # ApiResponse / Result / PageResult / 全局异常
│       │   └── config/ auth/      # JWT 拦截、角色策略、CORS、MyBatis-Plus
│       └── resources/
│           ├── application.properties
│           └── mapper/*.xml
├── admin-web/                     # Vue 2.7 + Element UI 2 前端（Vite，端口 5173）
│   │                              # 统一登录，按角色分流：管理员 / 医生 / 患者
│   └── src/{api,views,layout,router,utils,mixins,styles}
├── miniprogram/                   # 微信患者端小程序（微信开发者工具导入此目录）
├── sql/system.sql                 # 数据库全量脚本（会 DROP 重建，勿在正式库执行）
├── docs/api.md                    # REST 接口契约（唯一事实源）
└── README.md
```

**本次整理做了什么**

- 去掉了 `backend/backend/` 双层嵌套，Maven 工程根现在就是 `backend/`（用 `git mv` 完成，历史未丢失）。
- `admin-web/` 从空占位 README 变为可运行的完整前端工程。
- `.gitignore` 按用途分组，并把上传目录路径改为 `/backend/uploads/`。
- 补齐了 `docs/api.md` 第 12 节中后端原本缺失的管理接口：`dashboard/stats`、`menus`、`medicines`、`users`、`logs/*`。
- 后端 Java 包结构保持不动：新旧两套管理接口并存是有意设计——`/api/admin/**` 是契约接口，`/admin/**` 仅作迁移兼容且承载删除操作，全部共用同一套 Service。小程序目录也保持原样。

## 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.9+（或使用 `backend/mvnw`） |
| Node.js | 18+（实测 22 可用） |
| MySQL | 8+ |
| Redis | 可选，Redis 不可用时核心流程会降级 |
| 微信开发者工具 | 仅患者端需要 |

## 启动顺序

### 1. 初始化数据库

`sql/system.sql` 会删除并重建业务表，**只应在新建或可丢弃的本地数据库中执行**。

```bash
mysql -u root -p --default-character-set=utf8mb4 < sql/system.sql
```

已有数据库请先备份，再在 hospital_booking 库中按编号执行 sql/migrations/ 下尚未应用的增量脚本。新装直接使用 sql/system.sql。

数据库连接信息在 `backend/src/main/resources/application.properties`，请按本机环境调整（用户名、密码、地址、Redis），不要把新密钥提交到仓库。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
# 或 Windows: .\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

服务监听 `http://localhost:9090`，无 context-path。

> **为什么必须显式传端口**：Spring Boot 的宽松绑定会把环境变量 `SERVER_PORT` / `SERVER__PORT` 识别为 `server.port`。部分 IDE 或终端工具会注入这类变量（例如本机会话里的 `SERVER__PORT=54830`），从而覆盖 `application.properties` 的 9090 并导致端口冲突。命令行参数优先级最高，显式写上 `--server.port=9090` 可稳定复现。
> 在 IDEA 里运行时，请在 Run Configuration 的 VM/Program arguments 中确认没有注入同名端口变量。

> **IDEA 用户注意**：若之前导入的是 `backend/backend/pom.xml`，请以 `backend/pom.xml` 重新 "Add as Maven Project" 并 Reload Maven / Rebuild，旧路径已不存在。命令行编译通过即可确认源码无问题：`mvn -o clean compile -DskipTests`。

新预约的支付保留期默认 15 分钟，可通过 APPOINTMENT_PAYMENT_HOLD_MINUTES（1 至 1440）调整。后端默认每 15 秒释放超时订单的号源，可通过 APPOINTMENT_EXPIRY_SCAN_DELAY 调整扫描间隔（例如 PT15S）。迁移 005 会按创建时间为既有未支付订单补齐 15 分钟截止时间，并重新统计已预约数，建议在维护窗口执行。

如有医院人工导诊台联系电话，可通过环境变量 TRIAGE_HELP_PHONE 配置。患者在就医导航结果可直接联系导诊台。

### 3. 启动前端（管理员 / 医生 / 患者统一登录入口）

```bash
cd admin-web
cp .env.example .env        # Windows PowerShell: Copy-Item .env.example .env
npm install
npm run dev
```

打开 <http://localhost:5173>。登录页提供三个演示账号的一键填入按钮；登录后按 `LoginUser.role` 自动进入对应工作台，路由守卫会阻止跨角色访问。Vite 会把 `/api`、`/admin`、`/uploads` 代理到 `VITE_BACKEND_URL`（默认 `http://localhost:9090`），开发时无需修改后端 CORS 白名单。

生产构建：`npm run build` → 产物在 `admin-web/dist/`，由 Nginx 等静态服务器托管时，需要把 `/api` 与 `/admin` 反向代理到后端；也可在 `.env` 里把 `VITE_API_BASE_URL` 写成完整的后端 `/api` 地址。

### 4. 小程序（可选）

```bash
cd miniprogram
npm install
npm run build:npm
```

用微信开发者工具导入 `miniprogram/`。开发者工具本地联调时，可勾选「不校验合法域名」并在 `miniprogram/utils/config.js` 中使用本机可达的后端地址。若使用当前局域网 IP，手机必须连接电脑所在的同一网络；蜂窝网络无法访问该地址。手机普通预览不会继承开发者工具的本地不校验选项；长期稳定预览和体验版应使用可访问的 HTTPS 域名，在微信公众平台「开发管理 → 开发设置 → 服务器域名」配置 `request 合法域名`，头像上传还需配置 `uploadFile 合法域名`。开发阶段可在同一局域网使用「真机调试」验证。

## 默认账号与三端页面

| 角色 | 账号 | 密码 | 登录后首页 | 可访问接口域 |
|---|---|---|---|---|
| 管理员 | `admin` | `admin` | `/admin/dashboard` | `/api/admin/**`、`/admin/**` |
| 医生 | `doctorzmh` | `123` | `/doctor/dashboard` | `/api/doctor/**` |
| 患者 | `wx_ogHVa5P9lsJ4o0rAE2EKJeCD-OAw` | `123` | `/patient/home` | `/api/appointments`、`/api/notifications`、`/api/wallet`、`/api/prescriptions` |

- 登录接口 `POST /api/auth/login` **对三个角色都开放**，返回 `data.token` 与 `data.user.role`；后续请求带 `Authorization: Bearer <token>`。前端只按角色切换页面，后端 `RoleAccessPolicy` 才是真正的权限边界。
- Web 与小程序共用后端 `sys_user` 账号。Web 使用账号密码登录；小程序患者使用 `/api/auth/wx-login` 获取登录态。微信登录后，患者可在“我的”页设置网页登录密码，复制当前账号后即可在 Web 端登录同一个患者账号。
- 医生账号共 36 个（`doctor+姓名拼音首字母`），密码统一 `123`；医生账号必须绑定 `doctor_id`，否则登录会被后端拒绝。

### 前端页面分组

| 角色 | 页面 |
|---|---|
| 管理员 | 数据概览、科室、医生、排班、预约、药品、用户、系统日志、菜单 |
| 医生 | 工作台概览、我的排班、就诊列表、接诊工作台（病历 + 开处方）、药品目录 |
| 患者 | 我的首页（含智能分诊）、预约挂号、我的预约、病历与处方、消息通知、余额与充值、个人资料 |

## 前后端接口对应关系

| 后台页面 | 主要接口 |
|---|---|
| 登录 | `POST /api/auth/login`、`GET /api/auth/me` |
| 数据概览 / 侧边菜单 | `GET /api/admin/dashboard/stats`、`GET /api/admin/menus` |
| 科室管理 | `GET/POST /api/admin/departments`、`PUT /api/admin/departments/{id}`、`PUT .../{id}/status`、`DELETE /admin/department/delete?id=` |
| 医生管理 | `GET/POST /api/admin/doctors`、`PUT /api/admin/doctors/{id}`、`PUT .../{id}/status`、`DELETE /admin/doctor/delete?id=` |
| 排班管理 | `GET/POST /api/admin/schedules`、`POST .../batch`、`PUT .../{id}`、`PUT .../{id}/status`、`DELETE /admin/schedule/delete/{id}` |
| 预约管理 | `GET /api/admin/appointments`、`PUT .../{id}/cancel`、`PUT .../{id}/check-in`、`PUT .../{id}/status`、`GET .../export` |
| 药品管理 | `GET/POST /api/admin/medicines`、`PUT/DELETE /api/admin/medicines/{id}` |
| 用户管理 | `GET /api/admin/users`、`PUT /api/admin/users/{id}/status` |
| 系统日志 | `GET /api/admin/logs/login`、`GET /api/admin/logs/operation` |
| 公共下拉 | `GET /api/departments`、`GET /api/doctors`、`GET /api/schedules` |
| 医生工作台 | `GET /api/doctor/dashboard`、`/api/doctor/schedules`、`/api/doctor/appointments`、`/api/doctor/appointments/{id}`、`POST .../start`、`PUT .../clinical`、`POST .../complete`、`GET /api/doctor/medicines` |
| 患者 · 挂号 | `POST /api/appointments`、`POST /api/appointments/{id}/pay`、`PUT .../{id}/cancel`、`PUT .../{id}/check-in`、`POST /api/triage/analyze` |
| 患者 · 就诊 | `GET /api/appointments/{id}/consultation`、`GET /api/appointments/{id}/prescription`、`POST /api/prescriptions/{id}/pay`、`POST /api/appointments/{id}/review` |
| 患者 · 账户 | `GET /api/notifications` + `/unread-count` + `/read-all`、`POST /api/wallet/recharge`、`GET /api/wallet/logs`、`PUT /api/profile`、`POST /api/files/upload` |

所有列表页都由后端分页：请求带 `current` / `size`，响应 `data = { records, total, current, size }`，前端 `el-pagination` 直接绑定，不在前端对全量数据切片。医生「我的排班」按日期区间返回列表（后端上限 200 条），不分页。

## 操作日志（AOP 审计）

后台「系统日志 → 操作日志」的数据由 AOP 切面自动写入 `sys_operation_log`，不需要在每个业务方法里手写日志代码。

| 文件 | 作用 |
|---|---|
| `admin/log/OperationLog.java` | 自定义注解：覆盖自动推导的模块 / 动作名，或 `ignore = true` 排除某个接口 |
| `admin/log/OperationLogAspect.java` | `@Around` 环绕通知，切点为所有 `@PostMapping / @PutMapping / @DeleteMapping / @PatchMapping` |
| `admin/log/OperationLogWriter.java` | 落库，使用 `REQUIRES_NEW` 独立事务 |
| `admin/log/AopConfig.java` | `@EnableAspectJAutoProxy` 显式开启自动代理 |

- 每条记录：操作人（JWT）、模块、动作、HTTP 方法、URL、客户端 IP、耗时毫秒、成功/失败、时间。模块按 Controller 类名推导，动作按方法名 + HTTP 动词推导；`AdminMedicineController.create` 演示了用 `@OperationLog(module = "药品管理", action = "新增药品")` 精确命名。
- **成功/失败判定不只看异常**：本项目旧接口存在 HTTP 200 + `code = 500` 的情况，切面同时检查 `ApiResponse` / `Result` / `ResponseEntity` 的 code 与状态码。
- 日志写入在 `finally` 中且用独立事务，业务回滚或失败时失败记录仍会保留；写入异常只告警，绝不影响业务结果。
- 登录相关接口由 `sys_login_log` 单独记录，切面里已排除，避免重复。
- 依赖说明：Spring Boot 4 已移除 `spring-boot-starter-aop`，pom 里直接引入 `org.aspectj:aspectjweaver:1.9.25.1`（本地仓库已有），配合 spring-context 自带的 spring-aop 即可。
- 查询接口：`GET /api/admin/logs/operation`，支持 `current/size/username/module/status/startDate/endDate` 筛选，前端分页展示。

> 改完后端代码务必 Rebuild + 重启服务，本项目多次出现运行的是旧字节码、切面「静默不生效」的情况。

## 响应约定（重要）

- 统一响应体为 `{ code, data, message }`，成功 `code === 200`；分页为 `records/total/current/size`。
- 新 `/api/admin/**` 接口：HTTP 状态码与业务码一致（400/401/403/404/409）。
- 旧 `/admin/**` 删除接口：失败时仍是 HTTP 200 + `code = 500` + `message`。
- 前端 `src/utils/request.js` 对两种情况统一按 `code` 判断，并把 `data` 直接返回给页面。

## 常见问题

- **改了后端代码但行为没变**：本项目出现过多次 IDEA 编译产物与运行 classpath 不同步，改完请 Rebuild + 重启服务，必要时先用 `mvn -o clean compile` 验证。
- **页面提示「无法连接后端服务」**：确认后端已在 9090 启动，且 `.env` 中 `VITE_BACKEND_URL` 指向正确地址。
- **接口 401**：令牌过期（24h）或被吊销，重新登录即可。
- **接口 403**：`RoleAccessPolicy` 按角色隔离接口域，例如医生访问 `/api/admin/**` 或患者访问 `/api/doctor/**` 都会被后端拒绝；前端路由守卫会提前把越权访问导回本角色首页。
- **患者账号登录**：演示患者可直接使用表格中的账号密码。真实微信用户首次登录后，可在小程序“我的”页设置网页登录密码；Web 与小程序随后使用同一患者账号。
