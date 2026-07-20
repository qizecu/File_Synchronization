# =================================================
# 前端多阶段构建：Node 构建 + Nginx 运行
# =================================================
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ==================== 运行阶段 ====================
FROM nginx:1.25-alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
