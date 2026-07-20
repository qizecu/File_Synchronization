---
description: 生成 Java 或 Vue 代码时生效
---

# 代码规范

## 命名
- Controller：{Entity}Controller
- Service：{Entity}Service + {Entity}ServiceImpl
- Mapper：{Entity}Mapper
- DTO 请求：{Entity}CreateDTO / {Entity}QueryDTO
- DTO 响应：{Entity}VO

## 接口规范
- 基础路径：/api
- 统一返回：Result<T>，字段 code, message, data
- 分页参数：page, size
- 所有入参用 @Valid + DTO 校验
- RESTful 风格：GET 查询、POST 新增、PUT 修改、DELETE 删除

## 数据库规范
- 所有表必须有 id, created_at, updated_at, is_deleted
- id 用 Long 自增
- 软删除，不物理删除
- Redis key 必须设置 TTL

## 硬性约束（AI 容易忽略的）
- Controller 不写业务逻辑，只调用 Service
- 异常用 BusinessException，不写 try-catch
- 返回前端用 VO，不返回 Entity
- 不要使用 any 类型（TypeScript）
- 不要同步阻塞写法
- 避免多层嵌套，提前返回