# Recipe Hub

Web công thức nấu ăn: Vue 3 + Spring Boot 4 + PostgreSQL.

## Yêu cầu

- JDK 25
- Node 22
- Docker

## Chạy

```bash
cp .env.example .env          # sửa tài khoản admin nếu muốn
docker compose up -d          # Postgres (localhost:5434) + Mailpit
cd backend && ./mvnw spring-boot:run      # http://localhost:8080
cd frontend && npm install && npm run dev # http://localhost:5173
```

Email (xác thực tài khoản, quên mật khẩu) xem tại Mailpit: http://localhost:8025

## Test

```bash
cd backend && ./mvnw verify                          # cần Docker cho Testcontainers
cd frontend && npm run test && npm run type-check
```
