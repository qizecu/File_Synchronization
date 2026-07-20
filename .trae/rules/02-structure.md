---
description: 涉及项目结构或新建文件时生效
---

# 项目结构

## 后端目录
backend/src/main/java/com/example/picmanager/
├── controller/    → API 路由，只做参数校验和调用 Service
├── service/       → 业务逻辑（接口 + Impl 实现类）
├── mapper/        → MyBatis-Plus Mapper
├── entity/        → 数据库实体
├── dto/           → 请求对象（CreateDTO / QueryDTO）和响应对象（VO）
└── config/        → 配置类

## 前端目录
frontend/src/
├── views/         → 页面（PascalCase，如 ImageList.vue）
├── components/    → 组件（Base 前缀，如 BasePagination.vue）
├── api/           → 接口封装（小写模块名，如 image.ts）
├── router/        → 路由配置
└── store/         → Pinia 状态管理（小写模块名）

## SQL
sql/               → 建表脚本和初始化数据