# ═══════════════════════════════════════════════════════════════════
#  EduCore Frontend — Dockerfile
#  Stage 1: Build Angular  |  Stage 2: Serve with Nginx
# ═══════════════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build -- --configuration=production

# ── Stage 2: Nginx ──────────────────────────────────────────────────
FROM nginx:alpine

COPY --from=builder /app/dist/educore/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
