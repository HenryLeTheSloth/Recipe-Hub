# Recipe Hub — Giai đoạn 1: Tài khoản & phân quyền — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Đăng ký, xác thực email, đăng nhập session, quên/đổi mật khẩu, RBAC động (User–Role–Permission) và trang admin quản lý user/role.

**Architecture:** Spring Security với session lưu trong Postgres (Spring Session JDBC), đăng nhập bằng endpoint JSON tự viết lưu `SecurityContext` vào session. Authority = mã permission. Mọi lỗi đi qua một `ApiException` + enum `ErrorCode` → `ProblemDetail`. Frontend: store Pinia `auth` + route guard theo permission.

**Tech Stack:** Spring Boot 4.2 (Security 7, Session JDBC, Mail, Data JPA, Flyway), MapStruct; Vue 3, Pinia, Vue Router, PrimeVue (admin), Vitest.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md` (§3, §4, §9)

## Global Constraints

- Java 25, Spring Boot 4.2.0, MapStruct 1.6.x. Không Lombok; DTO là `record`.
- Boot 4: `@MockitoBean`, Jackson 3 (`tools.jackson.*`); test slice ở starter test riêng — IDE auto-import.
- Package `com.recipehub.<feature>`. Schema chỉ qua Flyway; `ddl-auto=validate`.
- Lỗi API: `throw new ApiException(ErrorCode.X, "…")` — không tạo class exception mới.
- Postgres 5434, Mailpit 1025/8025, backend 8080, frontend 5173.
- Test không gọi mạng ngoài: `MailService` luôn là `@MockitoBean` trong test.
- Cổng: `./mvnw verify` và `npm run test && npm run type-check` xanh 100%.
- Chữ giao diện và `detail` của lỗi bằng tiếng Việt.

## Review Focus

1. Email khác hoa/thường hoặc có khoảng trắng (`"  Foo@X.com "`) → đăng ký lần 2 bị `EMAIL_TAKEN`; đăng nhập bằng `"FOO@x.com"` vẫn thành công. → Task 3, Task 4
2. `forgot-password` / `resend-verification` với email không tồn tại hoặc đã khóa → vẫn 202, không gửi mail (không lộ email nào tồn tại). → Task 4
3. Admin khóa user đang đăng nhập, hoặc đổi role/quyền của user → request kế tiếp của user đó trả 401 (session bị xóa, không giữ quyền cũ). → Task 5
4. Gỡ ADMIN khỏi admin cuối cùng, khóa/xóa admin cuối cùng, tự khóa/xóa/đổi role chính mình, sửa quyền role ADMIN → 409, không bao giờ mất quyền quản trị. → Task 5
5. Mật khẩu tiếng Việt có dấu dài 25 ký tự `"ệ"` (75 byte) → 400 `INVALID_PASSWORD` thay vì lỗi 500 từ BCrypt. → Task 3

---

### Task 1: Lỗi chuẩn hóa + SecurityConfig đầy đủ

**Files:**
- Create: `backend/src/main/java/com/recipehub/common/ErrorCode.java`, `ApiException.java`, `GlobalExceptionHandler.java`, `BaseEntity.java`
- Modify: `backend/src/main/java/com/recipehub/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/recipehub/common/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces:
  ```java
  public enum ErrorCode {           // (HttpStatus status)
    VALIDATION_ERROR(400), INVALID_PASSWORD(400), INVALID_TOKEN(400), WRONG_PASSWORD(400),
    UNAUTHENTICATED(401), BAD_CREDENTIALS(401),
    FORBIDDEN(403), EMAIL_NOT_VERIFIED(403), ACCOUNT_LOCKED(403),
    NOT_FOUND(404),
    EMAIL_TAKEN(409), USERNAME_TAKEN(409), ROLE_CODE_TAKEN(409), ROLE_IN_USE(409), SYSTEM_ROLE(409),
    LAST_ADMIN(409), SELF_ACTION(409), USER_HAS_CONTENT(409);
    public HttpStatus status();
  }
  public class ApiException extends RuntimeException { ApiException(ErrorCode code, String message); ErrorCode code(); }
  @MappedSuperclass public abstract class BaseEntity { Long id; Instant createdAt; Instant updatedAt; } // @CreationTimestamp/@UpdateTimestamp
  ```
  Các giai đoạn sau **thêm** hằng vào `ErrorCode`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`): `ApiException` → ProblemDetail(status, detail=message) + property `code`; `MethodArgumentNotValidException` → 400 `VALIDATION_ERROR` + property `errors: Map<field, message>`; `AccessDeniedException` → 403 `FORBIDDEN`. Không có handler bắt `Exception` chung.
- `SecurityConfig`: permitAll `GET /api/health`, `GET /api/auth/csrf`, `POST /api/auth/{register,login,verify-email,resend-verification,forgot-password,reset-password}`; còn lại authenticated. `@EnableMethodSecurity`. 401 qua `HttpStatusEntryPoint`. Logout: `logoutUrl("/api/auth/logout")`, success handler trả 204. Bean `PasswordEncoder` = `PasswordEncoderFactories.createDelegatingPasswordEncoder()`, `AuthenticationManager` từ `DaoAuthenticationProvider`, `SecurityContextRepository` = `HttpSessionSecurityContextRepository`.

- [ ] **Step 1: Viết test fail**

```java
// Unit test, gọi thẳng handler
@Test void apiExceptionBecomesProblemWithCode() {
  ProblemDetail p = handler.handleApi(new ApiException(ErrorCode.EMAIL_TAKEN, "Email đã được dùng"));
  assertThat(p.getStatus()).isEqualTo(409);
  assertThat(p.getDetail()).isEqualTo("Email đã được dùng");
  assertThat(p.getProperties()).containsEntry("code", "EMAIL_TAKEN");
}
@Test void validationErrorListsFields() { /* MethodArgumentNotValidException với FieldError("req","email","Bắt buộc") → 400, code VALIDATION_ERROR, errors={email=Bắt buộc} */ }
@Test void accessDeniedIs403() { /* → 403, code FORBIDDEN */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest` — Expected: lỗi biên dịch.
- [ ] **Step 3: Implement** các file ở trên.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS` (test GĐ0 vẫn xanh).
- [ ] **Step 5: Commit** — `git commit -am "feat(backend): problem-detail errors and security config"`

---

### Task 2: Schema RBAC + entity + repository

**Files:**
- Create: `db/migration/V2__users_rbac.sql`, `V3__seed_rbac.sql`
- Create: `com/recipehub/user/{User,Role,Permission,UserToken,TokenType}.java`, `{UserRepository,RoleRepository,PermissionRepository,UserTokenRepository}.java`
- Test: `backend/src/test/java/com/recipehub/user/UserRepositoryTest.java`

**Interfaces:**
- Schema theo spec §4 cho `users` (chưa có `avatar_media_id` — GĐ2 thêm), `roles`, `permissions`, `user_roles`, `role_permissions`, `user_tokens`, đúng ON DELETE trong spec.
- Seed: 9 permission + mô tả tiếng Việt; role `ADMIN`, `MODERATOR`, `MEMBER` (`system = true`) gán quyền đúng bảng spec §3.
- Entity: `User` (`@ManyToMany(fetch=LAZY) Set<Role> roles`), `Role` (`code, name, description, boolean system, Set<Permission> permissions`), `Permission (code, description)`, `UserToken (User user, TokenType type, String tokenHash, Instant expiresAt, Instant usedAt)`.
- Repository:
  ```java
  boolean existsByEmail(String email); boolean existsByUsername(String username);
  @EntityGraph(attributePaths = {"roles", "roles.permissions"}) Optional<User> findWithAuthoritiesByEmail(String email);
  @Query /* lower(email|username|displayName) LIKE lower(concat('%', :q, '%')) */ Page<User> search(String q, Pageable p);
    // service đổi q null thành "" — không dùng ":q is null" (Postgres không suy ra kiểu tham số null)
  long countByRoles_Code(String code);
  List<User> findAllByRoles_Id(Long roleId);
  // RoleRepository
  Optional<Role> findByCode(String code); List<Role> findAllByCodeIn(Collection<String> codes);
  // PermissionRepository
  List<Permission> findAllByCodeIn(Collection<String> codes);
  // UserTokenRepository
  Optional<UserToken> findByTokenHash(String hash);
  @Modifying @Query("update UserToken t set t.usedAt = :now where t.user.id = :userId and t.type = :type and t.usedAt is null")
  int invalidateUnused(Long userId, TokenType type, Instant now);
  ```

- [ ] **Step 1: Viết test fail** — `@DataJpaTest @AutoConfigureTestDatabase(replace = NONE) @Import(TestcontainersConfiguration.class)`

```java
@Test void seedHasRolesAndPermissions() {
  assertThat(permissions.count()).isEqualTo(9);
  assertThat(roles.findByCode("MEMBER").orElseThrow().getPermissions()).extracting(Permission::getCode)
    .containsExactlyInAnyOrder("RECIPE_WRITE", "COMMUNITY_PARTICIPATE", "AI_BOWL_USE", "MEDIA_UPLOAD");
  assertThat(roles.findByCode("ADMIN").orElseThrow().getPermissions()).hasSize(9);
}
@Test void loadsAuthoritiesWithUser() { /* user có MODERATOR → findWithAuthoritiesByEmail → permission codes chứa CONTENT_MODERATE, USER_READ */ }
@Test void duplicateEmailRejectedByDb() { /* save 2 user cùng email + flush → DataIntegrityViolationException */ }
@Test void roleInUseCannotBeDeleted() { /* role gán cho user, delete + flush → DataIntegrityViolationException */ }
@Test void searchIsCaseInsensitive() { /* displayName "Bếp Trưởng", search("bếp") → 1 kết quả */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest=UserRepositoryTest` — Expected: FAIL.
- [ ] **Step 3: Implement** migration, entity, repository.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(user): rbac schema, seed and repositories"`

---

### Task 3: Đăng ký, token, mail, xác thực email

**Files:**
- Create: `com/recipehub/user/{AppUserDetails,AppUserDetailsService,Passwords,TokenService,MailService,AuthService}.java`, `dto/RegisterRequest.java`
- Test: `user/PasswordsTest.java`, `user/TokenServiceTest.java`, `user/AuthServiceTest.java`

**Interfaces:**
- Consumes: repository từ Task 2, `Clock` (GĐ0), `ApiException`.
- Produces:
  ```java
  public record AppUserDetails(Long id, String email, String passwordHash, boolean verified, boolean locked,
                               Set<String> permissions) implements UserDetails, Serializable
    // getUsername()=email, isEnabled()=verified, isAccountNonLocked()=!locked, authorities = permissions
    public boolean can(String permission);
  public final class Passwords { static void validate(String raw); } // <8 ký tự hoặc >72 byte UTF-8 → INVALID_PASSWORD
  public class TokenService {
    String issue(User user, TokenType type);        // trả raw token; DB lưu sha256 hex; hạn VERIFY_EMAIL 24h, RESET_PASSWORD 30 phút
    User consume(String raw, TokenType type);       // sai/hết hạn/đã dùng/sai type → INVALID_TOKEN; set usedAt
    void invalidateUnused(User user, TokenType type);
  }
  public class MailService { void sendVerification(String to, String rawToken); void sendPasswordReset(String to, String rawToken); }
    // link: {app.frontend-url}/verify-email?token=… và /reset-password?token=…; from no-reply@recipehub.local
  public record RegisterRequest(@Email @NotBlank String email, @Pattern("^[a-zA-Z0-9_]{3,30}$") String username,
                                @NotBlank @Size(max=50) String displayName, @NotBlank String password)
  public class AuthService {
    void register(RegisterRequest r);       // email trim+lowercase; EMAIL_TAKEN / USERNAME_TAKEN; role MEMBER; gửi mail verify
    void verifyEmail(String token);
    void resendVerification(String email);  // chỉ gửi khi user tồn tại, chưa verify, không khóa; im lặng nếu không
  }
  ```

- [ ] **Step 1: Viết test fail**

```java
// PasswordsTest
@Test void acceptsEightAsciiChars() { assertThatNoException().isThrownBy(() -> Passwords.validate("abcdefgh")); }
@Test void rejectsSeven() { assertThatThrownBy(() -> Passwords.validate("abcdefg")).hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PASSWORD); }
@Test void acceptsExactly72Bytes() { Passwords.validate("ệ".repeat(24)); }           // 72 byte
@Test void rejects73PlusBytes() { assertThatThrownBy(() -> Passwords.validate("ệ".repeat(25))) /* INVALID_PASSWORD */; }

// TokenServiceTest — Mockito + Clock.fixed
@Test void storesHashNotRawToken() { /* captor UserToken: tokenHash == sha256Hex(raw) && != raw; raw base64url >= 43 ký tự */ }
@Test void consumeExpiredFails() { /* expiresAt = now-1s → INVALID_TOKEN */ }
@Test void consumeUsedFails() { /* usedAt != null → INVALID_TOKEN */ }
@Test void consumeWrongTypeFails() { /* token VERIFY_EMAIL consume RESET_PASSWORD → INVALID_TOKEN */ }
@Test void consumeMarksUsed() { /* usedAt == now */ }

// AuthServiceTest
@Test void registerNormalizesEmail() { /* "  Foo@X.com " → user.email == "foo@x.com" */ }
@Test void registerRejectsTakenEmail() { /* existsByEmail("foo@x.com") true → EMAIL_TAKEN */ }
@Test void registerAssignsMemberAndHashesPassword() { /* roles = {MEMBER}; passwordHash == encoder.encode(...) ; verifiedAt null */ }
@Test void registerSendsVerificationMail() { /* mail.sendVerification("foo@x.com", rawFromTokenService) */ }
@Test void resendIgnoresUnknownEmail() { /* verifyNoInteractions(mail) */ }
@Test void verifySetsVerifiedAt() { /* tokenService.consume → user.emailVerifiedAt == clock.instant() */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='PasswordsTest,TokenServiceTest,AuthServiceTest'` — Expected: lỗi biên dịch.
- [ ] **Step 3: Implement.** `AppUserDetailsService.loadUserByUsername(email)` dùng `findWithAuthoritiesByEmail(email.trim().toLowerCase())`, không thấy → `UsernameNotFoundException`.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(user): registration, tokens and email verification"`

---

### Task 4: AuthController — login session, me, quên/đổi mật khẩu

**Files:**
- Create: `com/recipehub/user/{AuthController,MeController,SessionService}.java`, `dto/{LoginRequest,MeResponse,EmailRequest,TokenRequest,ResetPasswordRequest,ChangePasswordRequest}.java`
- Modify: `AuthService.java` (thêm `forgotPassword`, `resetPassword`, `changePassword`, `me`)
- Test: `user/AuthFlowIT.java`

**Interfaces:**
- Produces endpoint (spec §9):
  | Method & path | Body | Kết quả |
  |---|---|---|
  | GET `/api/auth/csrf` | — | 204 (gọi `CsrfToken#getToken()` để cookie được set) |
  | POST `/api/auth/register` | RegisterRequest | 201 |
  | POST `/api/auth/verify-email` | `{token}` | 204 |
  | POST `/api/auth/resend-verification` | `{email}` | 202 |
  | POST `/api/auth/login` | `{email, password}` | 200 MeResponse |
  | POST `/api/auth/logout` | — | 204 |
  | POST `/api/auth/forgot-password` | `{email}` | 202 |
  | POST `/api/auth/reset-password` | `{token, newPassword}` | 204 |
  | GET `/api/me` | — | 200 MeResponse |
  | PUT `/api/me/password` | `{currentPassword, newPassword}` | 204 |
  ```java
  public record MeResponse(Long id, String email, String username, String displayName, String avatarUrl,
                           List<String> roles, List<String> permissions)   // avatarUrl null tới GĐ2
  public class SessionService { void invalidateAll(String email); }  // FindByIndexNameSessionRepository.findByPrincipalName → deleteById
  ```
- Login: `authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email.trim().toLowerCase(), pw))`; `DisabledException` → `EMAIL_NOT_VERIFIED`, `LockedException` → `ACCOUNT_LOCKED`, `BadCredentialsException` → `BAD_CREDENTIALS`. Thành công: đổi session id nếu đã có session (`request.changeSessionId()`), tạo `SecurityContext` mới, `securityContextRepository.saveContext(...)`.
- `resetPassword` xong → `sessionService.invalidateAll(email)`. `forgotPassword`: user tồn tại và không khóa → `invalidateUnused(RESET_PASSWORD)`, issue, gửi mail; ngược lại im lặng.

- [ ] **Step 1: Viết test fail** — `@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)`, `@MockitoBean MailService mail` (captor lấy raw token). Mọi POST dùng `.with(csrf())` trừ test CSRF.

```java
@Test void fullHappyPath() {
  // register → captor token → verify-email 204 → login 200, body.permissions chứa "RECIPE_WRITE"
  // → GET /api/me với session trả về 200 → logout 204 → GET /api/me 401
}
@Test void loginIsCaseInsensitive() { /* đăng ký "foo@x.com", login "  FOO@x.com " → 200 */ }
@Test void loginBeforeVerifyIs403() { /* code EMAIL_NOT_VERIFIED */ }
@Test void wrongPasswordIs401() { /* code BAD_CREDENTIALS */ }
@Test void postWithoutCsrfIs403() { /* POST /api/auth/login không .with(csrf()) → 403 */ }
@Test void sessionIsStoredInDatabase() { /* sau login: select count(*) from spring_session >= 1 */ }
@Test void forgotPasswordUnknownEmailIs202WithoutMail() { verify(mail, never()).sendPasswordReset(any(), any()); }
@Test void resetPasswordFlow() {
  // forgot → token → reset 204 → login mật khẩu cũ 401, mới 200; dùng lại token → 400 INVALID_TOKEN
}
@Test void secondResetRequestInvalidatesFirstToken() { /* forgot 2 lần → token1 → 400 INVALID_TOKEN, token2 → 204 */ }
@Test void changePasswordWrongCurrentIs400() { /* code WRONG_PASSWORD */ }
@Test void duplicateRegisterIs409() { /* "  Foo@X.com " sau "foo@x.com" → code EMAIL_TAKEN */ }
@Test void registerValidation() { /* username "a!" → 400 VALIDATION_ERROR, errors.username tồn tại */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest=AuthFlowIT` — Expected: FAIL (404 hoặc lỗi biên dịch).
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Kiểm tra tay** — chạy app, `curl` đăng ký → mở `http://localhost:8025` thấy mail có link verify.
- [ ] **Step 6: Commit** — `git commit -am "feat(auth): session login, password reset and me endpoints"`

---

### Task 5: Admin quản lý user & role + AdminBootstrap

**Files:**
- Create: `com/recipehub/user/{AdminUserService,RoleService,AdminUserController,AdminRoleController,AdminBootstrap}.java`, `dto/{AdminUserResponse,LockRequest,UserRolesRequest,RoleRequest,RoleResponse,PermissionResponse}.java`
- Test: `user/AdminUserServiceTest.java`, `user/RoleServiceTest.java`, `user/AdminControllerSecurityTest.java`, `user/AdminSessionIT.java`, `user/AdminBootstrapTest.java`

**Interfaces:**
- Consumes: `SessionService.invalidateAll(email)`, repository Task 2.
- Produces:
  | Method & path | Quyền | Body → Kết quả |
  |---|---|---|
  | GET `/api/admin/users?q&page&size` | USER_READ | Page<AdminUserResponse> |
  | PATCH `/api/admin/users/{id}/lock` | USER_MANAGE | `{locked}` → AdminUserResponse |
  | PUT `/api/admin/users/{id}/roles` | USER_MANAGE | `{roleCodes}` → AdminUserResponse |
  | DELETE `/api/admin/users/{id}` | USER_MANAGE | 204 / 409 `USER_HAS_CONTENT` |
  | GET `/api/admin/roles` | ROLE_MANAGE | List<RoleResponse> |
  | POST `/api/admin/roles` | ROLE_MANAGE | RoleRequest → 201 RoleResponse |
  | PUT `/api/admin/roles/{id}` | ROLE_MANAGE | RoleRequest → RoleResponse |
  | DELETE `/api/admin/roles/{id}` | ROLE_MANAGE | 204 / 409 |
  | GET `/api/admin/permissions` | ROLE_MANAGE | List<PermissionResponse> |
  ```java
  public record AdminUserResponse(Long id, String email, String username, String displayName, boolean locked,
                                  boolean emailVerified, List<String> roles, Instant createdAt)
  public record RoleRequest(@Pattern("^[A-Z][A-Z0-9_]{2,30}$") String code, @NotBlank @Size(max=50) String name,
                            @Size(max=200) String description, @NotNull Set<String> permissionCodes) // code bị bỏ qua khi PUT
  public record RoleResponse(Long id, String code, String name, String description, boolean system,
                             List<String> permissionCodes, long userCount)
  ```
- Luật (actor = `AppUserDetails`):
  - Tác động lên chính mình (lock, đổi role, xóa) → `SELF_ACTION`.
  - Khóa / xóa / gỡ ADMIN khỏi user đang là ADMIN khi `countByRoles_Code("ADMIN") == 1` → `LAST_ADMIN`.
  - Khóa, đổi role → `invalidateAll(target.email)`. Sửa quyền của role → `invalidateAll` cho mọi user mang role đó.
  - Role code không tồn tại → `NOT_FOUND`; permission code không tồn tại → `VALIDATION_ERROR`.
  - Sửa quyền role ADMIN, xóa role `system` → `SYSTEM_ROLE`; xóa role đang gán → `ROLE_IN_USE`; trùng code → `ROLE_CODE_TAKEN`.
  - Xóa user: `delete` + `flush`, bắt `DataIntegrityViolationException` → `USER_HAS_CONTENT` (GĐ4 mới có dữ liệu để test case này).
- `AdminBootstrap implements ApplicationRunner`: nếu `countByRoles_Code("ADMIN") == 0` và `app.admin.email`/`password` không rỗng → tạo user đã verify, username `admin`, role ADMIN. Rỗng → log WARN, bỏ qua.

- [ ] **Step 1: Viết test fail**

```java
// AdminUserServiceTest (Mockito)
@Test void cannotLockSelf() { /* actor.id == target.id → SELF_ACTION */ }
@Test void cannotRemoveAdminFromLastAdmin() { /* target có ADMIN, countByRoles_Code=1, roleCodes={MEMBER} → LAST_ADMIN */ }
@Test void cannotLockLastAdmin() { /* LAST_ADMIN */ }
@Test void lockInvalidatesSessions() { verify(sessions).invalidateAll("u@x.com"); }
@Test void changingRolesInvalidatesSessions() { /* … */ }
@Test void unknownRoleCodeIsNotFound() { /* … */ }
@Test void deleteWithContentIs409() { /* repo.flush() ném DataIntegrityViolationException → USER_HAS_CONTENT */ }

// RoleServiceTest
@Test void adminRolePermissionsAreImmutable() { /* PUT ADMIN với permissionCodes khác → SYSTEM_ROLE */ }
@Test void adminRoleRenameAllowed() { /* PUT ADMIN đổi name, giữ nguyên quyền → OK */ }
@Test void systemRoleCannotBeDeleted() { /* SYSTEM_ROLE */ }
@Test void roleInUseCannotBeDeleted() { /* ROLE_IN_USE */ }
@Test void updatingPermissionsInvalidatesMembersSessions() { /* 2 user mang role → invalidateAll × 2 */ }
@Test void duplicateCodeRejected() { /* ROLE_CODE_TAKEN */ }

// AdminControllerSecurityTest — @WebMvcTest({AdminUserController.class, AdminRoleController.class}) @Import(SecurityConfig)
@Test void anonymousIs401() {}
@Test @WithMockUser(authorities = "USER_READ") void readerCanListButNotLock() { /* GET 200; PATCH lock 403 */ }
@Test @WithMockUser(authorities = "USER_MANAGE") void managerCannotTouchRoles() { /* GET /api/admin/roles 403 */ }

// AdminSessionIT — @SpringBootTest + MockMvc thật
@Test void lockedUserLosesSessionImmediately() {
  // member đăng nhập (giữ MockHttpSession) → admin PATCH lock → member GET /api/me với session cũ → 401
}
@Test void roleChangeForcesRelogin() { /* admin gán MODERATOR cho member → session cũ 401; login lại → permissions chứa CONTENT_MODERATE */ }

// AdminBootstrapTest (Mockito)
@Test void createsAdminWhenNoneAndEnvSet() {}
@Test void idempotentWhenAdminExists() { verify(users, never()).save(any()); }
@Test void skipsWhenEnvBlank() {}
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw verify` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(admin): user and role management with session invalidation"`

---

### Task 6: Frontend — auth store, route guard, các trang tài khoản

**Files:**
- Create: `frontend/src/api/auth.ts`, `src/api/messages.ts`, `src/stores/auth.ts`, `src/router/guard.ts`, `src/utils/safeRedirect.ts`, `src/layouts/PublicLayout.vue`
- Create: `src/pages/{LoginPage,RegisterPage,VerifyEmailPage,ForgotPasswordPage,ResetPasswordPage,AccountPage,ForbiddenPage}.vue`
- Modify: `src/main.ts`, `src/router/index.ts`
- Test: `src/stores/__tests__/auth.spec.ts`, `src/router/__tests__/guard.spec.ts`, `src/utils/__tests__/safeRedirect.spec.ts`, `src/pages/__tests__/LoginPage.spec.ts`

**Interfaces:**
- Consumes: `api`, `ApiError` (GĐ0); endpoint Task 4.
- Produces:
  ```ts
  export type Permission = 'RECIPE_WRITE'|'COMMUNITY_PARTICIPATE'|'AI_BOWL_USE'|'MEDIA_UPLOAD'|'CONTENT_MODERATE'
    |'CATALOG_MANAGE'|'USER_READ'|'USER_MANAGE'|'ROLE_MANAGE'
  export interface Me { id: number; email: string; username: string; displayName: string; avatarUrl: string|null;
                        roles: string[]; permissions: Permission[] }
  // stores/auth.ts — useAuthStore
  state: { me: Me | null }
  getters: isLoggedIn: boolean
  actions: fetchMe(): Promise<void>  // 401 → me = null
           login(email, password): Promise<void>; logout(): Promise<void>
           can(p: Permission): boolean; canAny(ps: Permission[]): boolean
  // router/guard.ts
  export function guard(to: RouteLocationNormalized, auth: { isLoggedIn: boolean; canAny(ps: Permission[]): boolean }):
    true | RouteLocationRaw   // meta.auth & chưa đăng nhập → { path: '/login', query: { redirect: to.fullPath } }
                              // meta.permission (Permission | Permission[], cần ít nhất 1) thiếu → '/403'
  export const ADMIN_PERMISSIONS: Permission[] = ['USER_READ','ROLE_MANAGE','CATALOG_MANAGE','CONTENT_MODERATE']
  // utils/safeRedirect.ts
  export function safeRedirect(r: unknown): string  // chỉ nhận chuỗi bắt đầu '/' và không bắt đầu '//' hoặc '/\'; còn lại '/'
  // api/messages.ts
  export function messageFor(e: unknown): string   // map ErrorCode → câu tiếng Việt; mặc định e.message
  ```
- `main.ts`: `await api('/auth/csrf')`, `await useAuthStore().fetchMe()` trước `app.mount`.
- Route: `/login`, `/register`, `/verify-email` (đọc `?token`, tự gọi khi mount), `/forgot-password`, `/reset-password`, `/account` (`meta.auth`), `/403`.

- [ ] **Step 1: Viết test fail**

```ts
// auth.spec.ts — mock module '@/api/auth'
it('can() checks permissions of me', () => { /* me.permissions=['RECIPE_WRITE'] → can('RECIPE_WRITE') true, can('USER_READ') false */ })
it('fetchMe sets null on 401', async () => { /* getMe rejects ApiError(401) → me null, không throw */ })
it('logout clears me', async () => {})

// guard.spec.ts
it('redirects anonymous to login with redirect', () => {
  expect(guard(route('/account', { auth: true }), anon)).toEqual({ path: '/login', query: { redirect: '/account' } })
})
it('sends to /403 when permission missing', () => {
  expect(guard(route('/admin/users', { auth: true, permission: 'USER_READ' }), member)).toBe('/403')
})
it('accepts any of permission list', () => { /* permission: ADMIN_PERMISSIONS, user có CATALOG_MANAGE → true */ })

// safeRedirect.spec.ts
it.each([['/recipes', '/recipes'], ['//evil.com', '/'], ['/\\evil.com', '/'], ['https://evil.com', '/'], [undefined, '/']])(
  'safeRedirect(%s) = %s', (i, o) => expect(safeRedirect(i)).toBe(o))

// LoginPage.spec.ts
it('shows resend link when email not verified', async () => {
  // login rejects ApiError(403, code 'EMAIL_NOT_VERIFIED') → text "Email chưa được xác thực" + nút "Gửi lại email xác thực"
})
it('navigates to safe redirect after login', async () => { /* ?redirect=//evil.com → router.push('/') */ })
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.** Form hiển thị `fieldErrors` dưới từng ô; nút submit disable khi đang gửi.
- [ ] **Step 4: Chạy pass** — Run: `npm run test && npm run type-check` — Expected: xanh.
- [ ] **Step 5: Commit** — `git commit -am "feat(frontend): auth store, guards and account pages"`

---

### Task 7: Frontend — trang admin user & role

**Files:**
- Create: `frontend/src/api/admin.ts`, `src/layouts/AdminLayout.vue`, `src/pages/admin/{AdminUsersPage,AdminRolesPage}.vue`
- Modify: `src/router/index.ts` (route `/admin` lazy-load, `meta: { auth: true, permission: ADMIN_PERMISSIONS }`; `/admin/users` `USER_READ`; `/admin/roles` `ROLE_MANAGE`)
- Test: `src/layouts/__tests__/AdminLayout.spec.ts`, `src/pages/admin/__tests__/AdminRolesPage.spec.ts`

**Interfaces:**
- Consumes: endpoint Task 5, `useAuthStore().can`.
- `AdminLayout`: sidebar link chỉ hiện khi `can(...)` tương ứng (Người dùng ↔ USER_READ, Vai trò ↔ ROLE_MANAGE; GĐ2–3 thêm link khác).
- `AdminUsersPage`: PrimeVue DataTable lazy (page/size), ô tìm kiếm, nút khóa/mở khóa và dialog chọn role (chỉ hiện khi `can('USER_MANAGE')`), hiển thị `messageFor` khi lỗi (ví dụ `LAST_ADMIN`).
- `AdminRolesPage`: danh sách role, dialog tạo/sửa với checkbox permission (lấy từ `/admin/permissions`); role `ADMIN` → checkbox disabled; role `system` → nút xóa disabled.

- [ ] **Step 1: Viết test fail**

```ts
it('hides roles link without ROLE_MANAGE', () => { /* me.permissions=['USER_READ'] → không có text "Vai trò" */ })
it('disables permission checkboxes for ADMIN role', async () => { /* mở dialog sửa ADMIN → mọi checkbox disabled */ })
it('saves selected permission codes', async () => {
  // sửa role "EDITOR", tick CATALOG_MANAGE → api PUT /admin/roles/5 với json.permissionCodes chứa 'CATALOG_MANAGE'
})
it('disables delete for system roles', () => {})
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Cổng giai đoạn** — Run: `cd backend && ./mvnw verify && cd ../frontend && npm run test && npm run type-check` — Expected: tất cả xanh.
- [ ] **Step 5: Kiểm tra tay** — đặt `.env` (ADMIN_EMAIL/PASSWORD), chạy app: đăng ký member → verify qua Mailpit → login; login admin → khóa member → tab member bị đẩy về trạng thái chưa đăng nhập khi reload.
- [ ] **Step 6: Commit** — `git commit -am "feat(frontend): admin users and roles pages"`
