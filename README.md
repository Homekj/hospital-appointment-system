# Hospital-Appointment-Management

医院预约挂号管理系统课程项目，当前包含 Spring Boot 后端脚手架、微信患者端小程序、数据库初始化脚本和 REST API 契约。

## 目录说明

- `backend/backend/`：Spring Boot 后端
- `miniprogram/`：微信患者端小程序
- `admin-web/`：管理后台预留目录
- `sql/system.sql`：数据库初始化脚本
- `docs/api.md`：REST API 契约

## 环境要求

- JDK 17
- Maven 3.9+ 或仓库自带的 Maven Wrapper
- Node.js 20+ 与 npm
- MySQL 8+
- 微信开发者工具

## 数据库初始化

`sql/system.sql` 会删除并重建业务表，只应在新建或可丢弃的本地数据库中执行。

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/system.sql"
```

## 后端

进入 Spring Boot 项目目录运行测试：

```powershell
cd backend/backend
.\mvnw.cmd test
```

当前后端仍是基础脚手架，`application.properties` 尚未配置 MySQL 和 Redis 连接。执行 `spring-boot:run` 前需要先补充本地数据源配置。`docs/api.md` 描述的是目标接口契约，不代表接口已全部实现。

## 小程序

```powershell
cd miniprogram
npm install
npm run build:npm
```

随后使用微信开发者工具导入 `miniprogram/` 目录。`npm run build:npm` 会重新生成 `miniprogram_npm/`，仅在依赖变更时执行，并检查生成物改动是否需要提交。

## 管理后台与成品参照

`admin-web/` 当前只有占位 README，管理后台源码和启动命令尚未提交到本仓库。

成品界面参照：[登录页](http://192.168.0.48:5173/login)。该地址仅用于视觉和交互参照，不是本仓库提供的本地启动地址。

## API 文档

接口契约见 [docs/api.md](docs/api.md)。
