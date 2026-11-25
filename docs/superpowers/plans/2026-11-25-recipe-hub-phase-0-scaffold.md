# Recipe Hub — Giai đoạn 0: Dựng khung — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repo chạy được: Postgres + Mailpit bằng Docker, backend Spring Boot có health endpoint và bảng session, frontend Vue có `fetch` wrapper — tất cả có test xanh.

**Architecture:** Monorepo `backend/` + `frontend/` + `docker-compose.yml`. Backend sinh từ start.spring.io, frontend sinh từ create-vue. Frontend gọi `/api` qua proxy Vite tới cổng 8080.

**Tech Stack:** Java 25, Spring Boot 4.2.0, Maven wrapper, Flyway, Testcontainers; Vue 3.5, Vite 8, TypeScript, Pinia, Vue Router, Vitest, PrimeVue 5.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md`

## Global Constraints

- Java 25, Spring Boot 4.2.0, Spring AI 2.1.0 (GĐ5), MapStruct 1.6.x stable. Không Lombok; DTO là `record`.
- Boot 4: dùng `@MockitoBean` (không `@MockBean`), Jackson 3 (`tools.jackson.*`); test slice nằm trong các starter test riêng do initializr sinh — dùng IDE auto-import.
- Package `com.recipehub.<feature>`. Mọi thay đổi schema qua Flyway `backend/src/main/resources/db/migration/V{n}__*.sql`; `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`.
- Lỗi API: `throw new ApiException(ErrorCode.X, "thông điệp tiếng Việt")` (từ GĐ1) — không tạo class exception mới.
- Cổng: Postgres host **5434**, Mailpit SMTP 1025 / UI 8025, backend 8080, frontend 5173 (proxy `/api`).
- Test không gọi mạng ngoài (AI, SMTP thật).
- Cổng giai đoạn: `cd backend && ./mvnw verify` và `cd frontend && npm run test && npm run type-check` xanh 100%.
- Chữ trên giao diện bằng tiếng Việt.

## Review Focus

1. Máy đã có Postgres khác chạy ở 5433 (kanban-db) → compose phải dùng host port 5434, backend vẫn kết nối được (cấu hình + `docker compose ps`).
2. Backend tắt / mất mạng → `api()` ném `ApiError` status 0, message "Không kết nối được máy chủ", không treo hay ném TypeError.
3. Lỗi 4xx/5xx có body không phải JSON (proxy Vite trả HTML) → vẫn ném `ApiError` đúng status.
4. Response 204 → `api()` trả `undefined`, không lỗi parse JSON.
5. Chưa có cookie `XSRF-TOKEN` → request vẫn gửi, không header rỗng, không crash.

---

### Task 1: Repo, Docker Compose, README

**Files:**
- Create: `.gitignore`, `docker-compose.yml`, `.env.example`, `README.md`

**Interfaces:**
- Produces: Postgres `localhost:5434`, DB/user/password `recipehub`; Mailpit `localhost:1025` (SMTP), `http://localhost:8025` (UI). File `.env` ở gốc repo (không commit) được backend đọc ở Task 2.

- [ ] **Step 1: Viết `docker-compose.yml`**

Service `postgres` (image `postgres:17`, env `POSTGRES_DB/USER/PASSWORD=recipehub`, ports `5434:5432`, volume `pgdata`, healthcheck `pg_isready -U recipehub`). Service `mailpit` (image `axllent/mailpit`, ports `1025:1025`, `8025:8025`).

`.gitignore`: `.env`, `backend/target/`, `backend/uploads/`, `frontend/node_modules/`, `frontend/dist/`, `.idea/`, `.vscode/` (trừ `extensions.json`).

`.env.example` (đúng các khóa này, giá trị mẫu):
```
ADMIN_EMAIL=admin@recipehub.local
ADMIN_PASSWORD=ChangeMe123!
```

`README.md`: yêu cầu (JDK 25, Node 22, Docker), lệnh chạy `docker compose up -d`, `cd backend && ./mvnw spring-boot:run`, `cd frontend && npm run dev`, lệnh test của cổng giai đoạn, link Mailpit.

- [ ] **Step 2: Chạy và kiểm tra**

Run: `docker compose up -d && docker compose ps && docker compose exec postgres pg_isready -U recipehub`
Expected: cả hai service `running`, postgres `healthy`; dòng cuối `accepting connections`.

- [ ] **Step 3: Commit**

```bash
git init && git add . && git commit -m "chore: repo skeleton with postgres and mailpit"
```

---

### Task 2: Backend skeleton

**Files:**
- Create: `backend/` (sinh bằng initializr)
- Create: `backend/src/main/resources/application.yml` (xóa `application.properties`)
- Create: `backend/src/main/resources/db/migration/V1__spring_session.sql`
- Create: `backend/src/main/java/com/recipehub/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/recipehub/config/ClockConfig.java`
- Create: `backend/src/main/java/com/recipehub/health/HealthController.java`
- Test: `backend/src/test/java/com/recipehub/health/HealthControllerTest.java`, `backend/src/test/java/com/recipehub/BackendApplicationTests.java`

**Interfaces:**
- Produces: `GET /api/health` → `200 {"status":"UP"}` (không cần đăng nhập). Bean `Clock` = `Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))`. `TestcontainersConfiguration` (initializr sinh) — các test DB sau `@Import` nó. `SecurityConfig` khung (GĐ1 mở rộng): `/api/health` permitAll, còn lại authenticated, chưa đăng nhập → 401 qua `HttpStatusEntryPoint`, CSRF dạng SPA (cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`).

- [ ] **Step 1: Sinh project**

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d bootVersion=4.2.0 -d javaVersion=25 \
  -d groupId=com.recipehub -d artifactId=backend -d name=backend -d packageName=com.recipehub \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,security,session-jdbc,mail,testcontainers,devtools \
  -o backend.zip && unzip -q backend.zip -d backend && rm backend.zip
```

Thêm `org.mapstruct:mapstruct` 1.6.x vào dependencies và `mapstruct-processor` vào `annotationProcessorPaths` của `maven-compiler-plugin`. Khai báo `maven-failsafe-plugin` trong `<build><plugins>` (version do Boot parent quản lý) để các test `*IT` chạy trong `./mvnw verify`; chạy riêng một IT khi phát triển: `./mvnw test -Dtest=TenIT`.

- [ ] **Step 2: Cấu hình `application.yml`**

Các giá trị cố định: `spring.config.import: optional:file:../.env[.properties]`; datasource `jdbc:postgresql://localhost:5434/recipehub` user/password `recipehub`; `spring.jpa.hibernate.ddl-auto: validate`; `spring.jpa.open-in-view: false`; `spring.session.jdbc.initialize-schema: never`; `spring.mail.host: localhost`, `port: 1025`; `app.frontend-url: http://localhost:5173`; `app.storage.dir: uploads`; `app.admin.email: ${ADMIN_EMAIL:}`, `app.admin.password: ${ADMIN_PASSWORD:}`.

`V1__spring_session.sql`: copy nguyên văn schema Postgres của Spring Session:
```bash
cd backend && ./mvnw -q dependency:resolve && \
unzip -p $(find ~/.m2 -name 'spring-session-jdbc-*.jar' | grep -v sources | head -1) \
  org/springframework/session/jdbc/schema-postgresql.sql > src/main/resources/db/migration/V1__spring_session.sql
```

- [ ] **Step 3: Viết test fail**

```java
// HealthControllerTest — @WebMvcTest(HealthController.class) @Import(SecurityConfig.class)
@Test void healthIsPublic() {
  assertThat(mvc.get().uri("/api/health")).hasStatusOk()
      .bodyJson().extractingPath("$.status").isEqualTo("UP");
}
@Test void otherEndpointsRequireAuth() {
  assertThat(mvc.get().uri("/api/nope")).hasStatus(HttpStatus.UNAUTHORIZED);
}

// BackendApplicationTests — @SpringBootTest @Import(TestcontainersConfiguration.class)
@Test void flywayCreatedSessionTables() {
  Integer n = jdbc.queryForObject(
    "select count(*) from information_schema.tables where table_name = 'spring_session'", Integer.class);
  assertThat(n).isEqualTo(1);
}
```

(`mvc` là `MockMvcTester`; `jdbc` là `JdbcTemplate` được inject.)

- [ ] **Step 4: Chạy để thấy fail**

Run: `cd backend && ./mvnw verify`
Expected: FAIL — `HealthController` chưa tồn tại (lỗi biên dịch).

- [ ] **Step 5: Implement `HealthController`, `SecurityConfig`, `ClockConfig`**

`HealthController`: `@GetMapping("/api/health") Map<String,String> health()` trả `Map.of("status","UP")`.

- [ ] **Step 6: Chạy lại**

Run: `cd backend && ./mvnw verify`
Expected: `BUILD SUCCESS`, 3 test pass (cần Docker đang chạy cho Testcontainers).

- [ ] **Step 7: Chạy thật**

Run: `cd backend && ./mvnw spring-boot:run` rồi `curl -s localhost:8080/api/health`
Expected: `{"status":"UP"}`.

- [ ] **Step 8: Commit**

```bash
git add backend && git commit -m "feat(backend): spring boot skeleton with health endpoint and session schema"
```

---

### Task 3: Frontend skeleton + `fetch` wrapper

**Files:**
- Create: `frontend/` (sinh bằng create-vue)
- Modify: `frontend/vite.config.ts`, `frontend/package.json`, `frontend/src/main.ts`, `frontend/src/App.vue`, `frontend/src/router/index.ts`
- Create: `frontend/src/api/http.ts`, `frontend/src/pages/HomePage.vue`
- Test: `frontend/src/api/__tests__/http.spec.ts`

**Interfaces:**
- Produces (mọi giai đoạn sau dùng):
  ```ts
  export class ApiError extends Error {
    status: number            // 0 = không kết nối được
    code?: string             // ProblemDetail.code
    fieldErrors?: Record<string, string>  // ProblemDetail.errors
  }
  export function api<T>(path: string, init?: RequestInit & { json?: unknown }): Promise<T>
  ```
  `path` không có tiền tố, ví dụ `api('/health')` → gọi `/api/health`. `json` được `JSON.stringify` + header `Content-Type: application/json`; `body` là `FormData` thì để nguyên. Luôn `credentials: 'same-origin'`. Method khác GET/HEAD gửi header `X-XSRF-TOKEN` lấy từ cookie `XSRF-TOKEN` (decodeURIComponent) nếu có. 204 → `undefined`.
- Script: `npm run test` = `vitest run`.

- [ ] **Step 1: Sinh project**

```bash
npm create vue@latest frontend -- --ts --router --pinia --vitest --eslint
cd frontend && npm install && npm install primevue @primeuix/themes
```

Xóa các component/view/test mẫu (HelloWorld, TheWelcome, icons, AboutView). Thêm `"test": "vitest run"` vào `scripts`. `vite.config.ts`: `server.proxy['/api'] = 'http://localhost:8080'`. `main.ts`: đăng ký Pinia, router, `PrimeVue` với `theme: { preset: Aura }`. Route `/` → `HomePage.vue` (tiêu đề "Recipe Hub").

- [ ] **Step 2: Viết test fail**

```ts
// http.spec.ts — vi.stubGlobal('fetch', vi.fn()) trong beforeEach; document.cookie reset
it('prefixes /api and parses JSON', async () => {
  fetchMock.mockResolvedValue(new Response('{"a":1}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
  expect(await api('/x')).toEqual({ a: 1 })
  expect(fetchMock.mock.calls[0][0]).toBe('/api/x')
})
it('sends XSRF header on POST when cookie present', async () => {
  document.cookie = 'XSRF-TOKEN=abc%3D'
  fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
  await api('/x', { method: 'POST', json: { b: 2 } })
  const init = fetchMock.mock.calls[0][1]
  expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('abc=')
  expect(new Headers(init.headers).get('Content-Type')).toBe('application/json')
  expect(init.body).toBe('{"b":2}')
})
it('omits XSRF header when cookie missing', async () => { /* POST → header null, không throw */ })
it('returns undefined on 204', async () => { /* expect(await api('/x',{method:'DELETE'})).toBeUndefined() */ })
it('maps ProblemDetail to ApiError', async () => {
  fetchMock.mockResolvedValue(new Response(JSON.stringify({ status: 400, detail: 'Sai', code: 'VALIDATION_ERROR', errors: { email: 'Bắt buộc' } }),
    { status: 400, headers: { 'Content-Type': 'application/problem+json' } }))
  await expect(api('/x')).rejects.toMatchObject({ status: 400, code: 'VALIDATION_ERROR', message: 'Sai', fieldErrors: { email: 'Bắt buộc' } })
})
it('maps non-JSON error body', async () => { /* Response('<html>', 502) → ApiError status 502, code undefined */ })
it('maps network failure to status 0', async () => {
  fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
  await expect(api('/x')).rejects.toMatchObject({ status: 0, message: 'Không kết nối được máy chủ' })
})
it('passes FormData through without JSON content-type', async () => { /* body instanceof FormData, Content-Type null */ })
```

- [ ] **Step 3: Chạy để thấy fail**

Run: `cd frontend && npm run test`
Expected: FAIL — không import được `../http`.

- [ ] **Step 4: Implement `src/api/http.ts`**

- [ ] **Step 5: Chạy cổng giai đoạn**

Run: `cd frontend && npm run test && npm run type-check && npm run build`
Expected: 8 test pass, type-check và build không lỗi.

- [ ] **Step 6: Kiểm tra proxy**

Backend đang chạy, `npm run dev`, mở `http://localhost:5173/api/health`.
Expected: `{"status":"UP"}`.

- [ ] **Step 7: Commit**

```bash
git add frontend && git commit -m "feat(frontend): vue skeleton with api fetch wrapper"
```
