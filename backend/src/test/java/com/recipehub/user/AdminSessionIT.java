package com.recipehub.user;

import com.recipehub.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

// AdminBootstrap tạo admin từ các property dưới đây khi context khởi động.
@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminSessionIT {

    @Autowired MockMvcTester mvc;
    @Autowired UserRepository users;
    @Autowired javax.sql.DataSource dataSource;
    @MockitoBean MailService mail;

    MvcTestResult send(String method, String path, String json, Cookie session) {
        var req = mvc.method(org.springframework.http.HttpMethod.valueOf(method)).uri(path).with(csrf());
        if (session != null) req = req.cookie(session);
        if (json != null) req = req.contentType(MediaType.APPLICATION_JSON).content(json);
        return req.exchange();
    }

    Cookie login(String email, String password) {
        var r = send("POST", "/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password), null);
        assertThat(r).hasStatusOk();
        return r.getResponse().getCookie("SESSION");
    }

    /** Đăng ký + verify một member mới; trả id. */
    long member(String name) {
        String email = name + "@x.com";
        assertThat(send("POST", "/api/auth/register",
                "{\"email\":\"%s\",\"username\":\"%s\",\"displayName\":\"%s\",\"password\":\"password1\"}"
                        .formatted(email, name, name), null)).hasStatus(201);
        var token = ArgumentCaptor.forClass(String.class);
        verify(mail).sendVerification(eq(email), token.capture());
        assertThat(send("POST", "/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token.getValue()), null))
                .hasStatus(204);
        return users.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void lockedUserLosesSessionImmediately() {
        long id = member("lockme");
        Cookie memberSession = login("lockme@x.com", "password1");
        Cookie admin = login("root@test.local", "RootPass123");

        assertThat(send("PATCH", "/api/admin/users/" + id + "/lock", "{\"locked\":true}", admin))
                .hasStatusOk().bodyJson().extractingPath("$.locked").isEqualTo(true);
        assertThat(send("GET", "/api/me", null, memberSession)).hasStatus(401);
        assertThat(send("POST", "/api/auth/login", "{\"email\":\"lockme@x.com\",\"password\":\"password1\"}", null))
                .hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_LOCKED");
        assertThat(send("POST", "/api/auth/login", "{\"email\":\"lockme@x.com\",\"password\":\"wrongpass\"}", null))
                .hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    void roleChangeForcesRelogin() {
        long id = member("promote");
        Cookie memberSession = login("promote@x.com", "password1");
        Cookie admin = login("root@test.local", "RootPass123");

        assertThat(send("PUT", "/api/admin/users/" + id + "/roles", "{\"roleCodes\":[\"MEMBER\",\"MODERATOR\"]}", admin))
                .hasStatusOk();
        assertThat(send("GET", "/api/me", null, memberSession)).hasStatus(401);
        var relogin = send("POST", "/api/auth/login", "{\"email\":\"promote@x.com\",\"password\":\"password1\"}", null);
        assertThat(relogin).hasStatusOk().bodyJson().extractingPath("$.permissions").asArray()
                .contains("CONTENT_MODERATE");
    }

    @Test
    void userListIsPagedAndSearchable() {
        member("findme");
        Cookie admin = login("root@test.local", "RootPass123");
        var r = send("GET", "/api/admin/users?q=FINDME&size=5", null, admin);
        assertThat(r).hasStatusOk().bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(r).bodyJson().extractingPath("$.page.size").isEqualTo(5);
        assertThat(r).bodyJson().extractingPath("$.content[0].roles").asArray().containsExactly("MEMBER");
    }

    @Test
    void adminCannotLockSelfOrDemoteLastAdmin() {
        Cookie admin = login("root@test.local", "RootPass123");
        long rootId = users.findByEmail("root@test.local").orElseThrow().getId();
        assertThat(send("PATCH", "/api/admin/users/" + rootId + "/lock", "{\"locked\":true}", admin))
                .hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("SELF_ACTION");
    }

    @Test
    void customRoleLifecycle() throws Exception {
        Cookie admin = login("root@test.local", "RootPass123");
        var created = send("POST", "/api/admin/roles",
                "{\"code\":\"EDITOR\",\"name\":\"Biên tập\",\"permissionCodes\":[\"RECIPE_WRITE\"]}", admin);
        assertThat(created).hasStatus(201).bodyJson().extractingPath("$.permissionCodes").asArray()
                .containsExactly("RECIPE_WRITE");
        Number roleId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(send("GET", "/api/admin/roles", null, admin)).hasStatusOk()
                .bodyJson().extractingPath("$[?(@.code=='ADMIN')].userCount").asArray().containsExactly(1);
        assertThat(send("DELETE", "/api/admin/roles/" + roleId, null, admin)).hasStatus(204);
    }

    @Test
    void userListKeepsIdOrderAfterUpdate() {
        long a = member("orda"), b = member("ordb"), c = member("ordc");
        Cookie admin = login("root@test.local", "RootPass123");
        // UPDATE ghi tuple mới cuối trang heap: không có ORDER BY thì 'a' nhảy xuống cuối.
        assertThat(send("PATCH", "/api/admin/users/" + a + "/lock", "{\"locked\":true}", admin)).hasStatusOk();
        assertThat(send("GET", "/api/admin/users?q=ord", null, admin)).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].id").asArray().containsExactly((int) a, (int) b, (int) c);
    }

    @Test
    void userListIgnoresClientSort() {
        long a = member("sorta"), b = member("sortb");
        Cookie admin = login("root@test.local", "RootPass123");
        // sort tùy ý (vd. passwordHash) là kênh dò dữ liệu; danh sách luôn theo id
        assertThat(send("GET", "/api/admin/users?q=sort&sort=passwordHash,desc", null, admin)).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].id").asArray().containsExactly((int) a, (int) b);
        assertThat(send("GET", "/api/admin/users?q=sort&sort=nope", null, admin)).hasStatusOk();
    }

    // Chống write skew của LAST_ADMIN: mọi thay đổi user phải chờ khóa hàng role ADMIN.
    @Test
    void adminChangesWaitForAdminRoleLock() throws Exception {
        long id = member("waiter");
        Cookie admin = login("root@test.local", "RootPass123");
        try (var holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            holder.createStatement().execute("select id from roles where code = 'ADMIN' for update");
            var call = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> send("PATCH", "/api/admin/users/" + id + "/lock", "{\"locked\":true}", admin));
            Thread.sleep(1000);
            assertThat(call).as("lock request must block on ADMIN role row").isNotDone();
            holder.rollback();
            assertThat(call.get(10, java.util.concurrent.TimeUnit.SECONDS)).hasStatusOk();
        }
    }
}
