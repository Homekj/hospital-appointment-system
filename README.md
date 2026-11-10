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