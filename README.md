# 文件同步管理系统

将 MinIO / 华为 OBS 中的图片文件同步到本地服务器，提供 Web 界面进行文件浏览、下载和管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JWT |
| 前端 | Vue 3 + TypeScript + Element Plus + Pinia |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 + Redisson 分布式锁 |
| 部署 | Docker + Docker Compose + Nginx |

## 快速开始

### 方式一：Docker 一键部署（推荐）

```bash
# 本地开发环境（TEST 模式，限制同步 10 个文件）
./deploy.sh local

# 生产环境（FULL 模式）
./deploy.sh prod

# 停止/重启/查看日志
./deploy.sh stop
./deploy.sh restart
./deploy.sh logs
```

### 方式二：手动启动

**1. 启动 MySQL 和 Redis**

```bash
docker compose up -d mysql redis
```

**2. 启动后端**

```bash
cd backend
# 导入 SQL
mysql -u root -p < sql/sys_user.sql
# 启动
mvn spring-boot:run
```

**3. 启动前端**

```bash
cd frontend-vue3ts
npm install
npm run dev
```

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 主要功能

- **存储源管理**：支持 MinIO / 华为 OBS，测试连接、启用/禁用
- **同步引擎**：全量同步（断点续传）+ 增量同步 + TEST 模式 + MD5 校验
- **定时调度**：每天凌晨 2 点自动执行
- **文件浏览**：在线浏览已同步文件，图片预览、下载、批量打包
- **文件上传**：支持拖拽上传、多文件、格式校验
- **通知推送**：同步完成后推送钉钉 / 企业微信 Webhook
- **登录鉴权**：JWT Token + Spring Security
- **磁盘监控**：三级告警（80% / 90% / 95%）

## API 文档

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT Token |
| GET | `/api/auth/me` | 获取当前用户信息 |
| POST | `/api/auth/logout` | 退出登录 |

### 存储源

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/storage-sources` | 列表 |
| POST | `/api/storage-sources` | 新增 |
| PUT | `/api/storage-sources/{id}` | 编辑 |
| DELETE | `/api/storage-sources/{id}` | 删除 |
| POST | `/api/storage-sources/{id}/test-connect` | 测试连接 |

### 同步任务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/sync-tasks` | 任务列表（分页、筛选） |
| GET | `/api/sync-tasks/{id}` | 任务详情 |
| GET | `/api/sync-tasks/{id}/files` | 任务文件明细 |
| POST | `/api/sync-tasks/trigger/full/{sourceId}` | 手动触发全量同步 |
| POST | `/api/sync-tasks/trigger/incremental/{sourceId}` | 手动触发增量同步 |
| POST | `/api/sync-tasks/files/{fileId}/retry` | 重试失败文件 |
| GET | `/api/sync-tasks/{id}/status` | 快速查询状态（Redis 缓存） |

### 文件操作

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files/browse?path={path}` | 浏览目录 |
| GET | `/api/files/download?path={path}` | 单文件下载 |
| POST | `/api/files/download-batch` | 批量下载（ZIP 流式打包） |
| GET | `/api/files/preview?path={path}` | 图片预览 |
| POST | `/api/files/upload` | 上传图片（支持多文件） |

### 通知配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notify-configs` | 列表 |
| POST | `/api/notify-configs` | 新增 |
| PUT | `/api/notify-configs/{id}` | 编辑 |
| DELETE | `/api/notify-configs/{id}` | 删除 |

### Dashboard

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/stats` | 获取统计面板数据 |

## 环境变量说明

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MYSQL_ROOT_PASSWORD | root | MySQL root 密码 |
| JWT_SECRET | sync-manager-... | JWT 签名密钥（生产必改） |
| STORAGE_PATH | /data/file-sync | 文件存储基础路径 |
| SYNC_MODE | TEST / FULL | 同步模式 |
| FRONTEND_PORT | 80 | 前端端口 |

## 项目结构

```
project0/
├── backend/                  # 后端 (Spring Boot)
│   ├── src/main/java/.../
│   │   ├── config/           # 配置（Security、JWT、Redisson）
│   │   ├── controller/       # 控制器（Auth/Dashboard/File/SyncTask/...）
│   │   ├── service/
│   │   │   ├── SyncOrchestrator.java  # 核心同步引擎
│   │   │   ├── NotifyService.java     # 钉钉/企微通知
│   │   │   ├── DistributedLockService.java  # 分布式锁
│   │   │   └── adapter/      # MinIO/OBS 适配器
│   │   ├── sql/              # 数据库建表 SQL
│   │   └── application.yml
│   └── Dockerfile
├── frontend-vue3ts/          # 前端 (Vue 3)
│   ├── src/
│   │   ├── views/            # 页面组件
│   │   ├── api/              # API 封装
│   │   └── stores/           # Pinia 状态管理
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── deploy.sh
├── .env.example
└── .gitignore
```
