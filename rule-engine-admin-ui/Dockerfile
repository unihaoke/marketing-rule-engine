# syntax=docker/dockerfile:1
# ====================================================================
# 营销规则引擎 - 前端镜像（多阶段构建：Node 构建 + Nginx 运行）
# Vue 3 + Vite + Element Plus + ECharts
# ====================================================================

# ---------- 构建阶段 ----------
FROM node:20-alpine AS builder
WORKDIR /build

# 利用缓存：先复制 package 文件并安装依赖
COPY package.json package-lock.json* ./
# 有 lock 用 ci（可复现），否则降级 install
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

# 复制源码并构建产物到 dist/
COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:alpine

# 复制静态资源到 nginx 默认目录
COPY --from=builder /build/dist /usr/share/nginx/html

# 覆盖默认 server 配置：静态托管 + /api 反向代理到后端
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
