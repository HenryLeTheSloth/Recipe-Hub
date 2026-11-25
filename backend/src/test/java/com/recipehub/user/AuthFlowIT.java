package com.recipehub.user;

import com.recipehub.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIT {

    static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvcTester mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MailService mail;

    // ---- helpers ----

    MvcTestResult post(String path, String json, Cookie... cookies) {
        var req = mvc.post().uri(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json);
        if (cookies.length > 0) req = req.cookie(cookies);
        return req.exchange();
    }

    MvcTestResult put(String path, String json, Cookie session) {
        return mvc.put().uri(path).with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    MvcTestResult get(String path, Cookie session) {
        return mvc.get().uri(path).cookie(session).exchange();
    }

    /** Đăng ký user mới với email/username duy nhất; trả email. */
    String register(String password, boolean verify) {
        int n = SEQ.incrementAndGet();
        String email = "user" + n + "@x.com";
        assertThat(post("/api/auth/register", """
                {"email":"%s","username":"user%d","displayName":"User %d","password":"%s"}
                """.formatted(email, n, n, password))).hasStatus(201);
        if (verify) {
            var token = ArgumentCaptor.forClass(String.class);
            verify(mail, times(1)).sendVerification(eq(email), token.capture());
            assertThat(post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token.getValue())))
                    .hasStatus(204);
        }
        return email;
    }

    MvcTestResult login(String email, String password) {
        return post("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    Cookie session(MvcTestResult r) {
        Cookie c = r.getResponse().getCookie("SESSION");
        assertThat(c).as("SESSION cookie").isNotNull();
        return c;
    }

    String lastResetToken(int expectedCalls) {
        var token = ArgumentCaptor.forClass(String.class);
        verify(mail, times(expectedCalls)).sendPasswordReset(any(), token.capture());
        return token.getValue();
    }

    // ---- tests ----

    @Test
    void fullHappyPath() {
        String email = register("password1", true);
        var login = login(email, "password1");
        assertThat(login).hasStatusOk().bodyJson().extractingPath("$.permissions").asArray().contains("RECIPE_WRITE");
        assertThat(login).bodyJson().extractingPath("$.roles").asArray().containsExactly("MEMBER");
        Cookie s = session(login);
        assertThat(get("/api/me", s)).hasStatusOk().bodyJson().extractingPath("$.email").isEqualTo(email);
        assertThat(mvc.post().uri("/api/auth/logout").with(csrf()).cookie(s).exchange()).hasStatus(204);
        assertThat(get("/api/me", s)).hasStatus(401);
    }

    @Test
    void meWithoutSessionIs401() {
        assertThat(mvc.get().uri("/api/me").exchange()).hasStatus(401);
    }

    @Test
    void loginIsCaseInsensitive() {
        String email = register("password1", true);
        assertThat(login("  " + email.toUpperCase() + " ", "password1")).hasStatusOk();
    }

    @Test
    void loginBeforeVerifyIs403() {
        String email = register("password1", false);
        assertThat(login(email, "password1")).hasStatus(403)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_NOT_VERIFIED");
    }

    @Test
    void wrongPasswordIs401() {
        String email = register("password1", true);
        assertThat(login(email, "wrongpass")).hasStatus(401)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
        assertThat(login("nobody@x.com", "wrongpass")).hasStatus(401)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    void postWithoutCsrfIs403() {
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@x.com\",\"password\":\"password1\"}").exchange()).hasStatus(403);
    }

    @Test
    void sessionIsStoredInDatabase() {
        String email = register("password1", true);
        session(login(email, "password1"));
        Integer n = jdbc.queryForObject("select count(*) from spring_session where principal_name = ?",
                Integer.class, email);
        assertThat(n).isGreaterThanOrEqualTo(1);
    }

    @Test
    void forgotPasswordUnknownEmailIs202WithoutMail() {
        assertThat(post("/api/auth/forgot-password", "{\"email\":\"nobody@x.com\"}")).hasStatus(202);
        assertThat(post("/api/auth/resend-verification", "{\"email\":\"nobody@x.com\"}")).hasStatus(202);
        verify(mail, never()).sendPasswordReset(any(), any());
        verify(mail, never()).sendVerification(any(), any());
    }

    @Test
    void resetPasswordFlow() {
        String email = register("password1", true);
        Cookie old = session(login(email, "password1"));
        assertThat(post("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(email))).hasStatus(202);
        String token = lastResetToken(1);
        String body = "{\"token\":\"%s\",\"newPassword\":\"newpassword\"}".formatted(token);
        assertThat(post("/api/auth/reset-password", body)).hasStatus(204);
        assertThat(get("/api/me", old)).as("phiên cũ bị xóa sau khi đặt lại mật khẩu").hasStatus(401);
        assertThat(login(email, "password1")).hasStatus(401);
        assertThat(login(email, "newpassword")).hasStatusOk();
        assertThat(post("/api/auth/reset-password", body)).hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void secondResetRequestInvalidatesFirstToken() {
        String email = register("password1", true);
        String json = "{\"email\":\"%s\"}".formatted(email);
        post("/api/auth/forgot-password", json);
        String token1 = lastResetToken(1);
        post("/api/auth/forgot-password", json);
        String token2 = lastResetToken(2);
        assertThat(post("/api/auth/reset-password",
                "{\"token\":\"%s\",\"newPassword\":\"newpassword\"}".formatted(token1))).hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_TOKEN");
        assertThat(post("/api/auth/reset-password",
                "{\"token\":\"%s\",\"newPassword\":\"newpassword\"}".formatted(token2))).hasStatus(204);
    }

    @Test
    void changePasswordWrongCurrentIs400() {
        String email = register("password1", true);
        Cookie s = session(login(email, "password1"));
        assertThat(put("/api/me/password", "{\"currentPassword\":\"nope\",\"newPassword\":\"newpassword\"}", s))
                .hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("WRONG_PASSWORD");
        assertThat(put("/api/me/password", "{\"currentPassword\":\"password1\",\"newPassword\":\"newpassword\"}", s))
                .hasStatus(204);
        assertThat(login(email, "newpassword")).hasStatusOk();
    }

    @Test
    void changePasswordSignsOutOtherSessionsButKeepsCurrent() {
        String email = register("password1", true);
        Cookie current = session(login(email, "password1"));
        Cookie other = session(login(email, "password1"));
        assertThat(put("/api/me/password", "{\"currentPassword\":\"password1\",\"newPassword\":\"newpassword\"}", current))
                .hasStatus(204);
        assertThat(get("/api/me", other)).hasStatus(401);
        assertThat(get("/api/me", current)).hasStatusOk();
    }

    @Test
    void duplicateRegisterIs409() {
        String email = register("password1", false);
        assertThat(post("/api/auth/register", """
                {"email":"  %s ","username":"other_%d","displayName":"X","password":"password1"}
                """.formatted(email.toUpperCase(), SEQ.incrementAndGet()))).hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void registerValidation() {
        assertThat(post("/api/auth/register", """
                {"email":"v@x.com","username":"a!","displayName":"X","password":"password1"}
                """)).hasStatus(400).bodyJson().extractingPath("$.errors.username").isNotNull();
    }

    @Test
    void vietnamesePasswordOver72BytesIs400() {
        int n = SEQ.incrementAndGet();
        assertThat(post("/api/auth/register", """
                {"email":"p%d@x.com","username":"pw_%d","displayName":"X","password":"%s"}
                """.formatted(n, n, "ệ".repeat(25)))).hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_PASSWORD");
    }

    // Sai mật khẩu không được tiết lộ trạng thái tài khoản (chưa xác minh/bị khóa).
    @Test
    void unverifiedWrongPasswordIs401() {
        String email = register("password1", false);
        assertThat(login(email, "wrongpass")).hasStatus(401)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    void emailLongerThan100CharsCanLogIn() {
        int n = SEQ.incrementAndGet();
        String email = "long" + n + "a".repeat(55) + "@" + "d".repeat(60) + ".example.com"; // > 100 ký tự
        assertThat(post("/api/auth/register", """
                {"email":"%s","username":"long%d","displayName":"Long","password":"password1"}
                """.formatted(email, n))).hasStatus(201);
        var token = ArgumentCaptor.forClass(String.class);
        verify(mail).sendVerification(eq(email), token.capture());
        assertThat(post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token.getValue()))).hasStatus(204);
        assertThat(login(email, "password1")).hasStatusOk();
    }

    @Test
    void emailOver254CharsIs400() {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(59) + ".com";
        assertThat(post("/api/auth/register", """
                {"email":"%s","username":"toolong","displayName":"X","password":"password1"}
                """.formatted(email))).hasStatus(400);
    }

    @Test
    void usernameIsUniqueIgnoringCase() {
        int n = SEQ.incrementAndGet();
        assertThat(post("/api/auth/register", """
                {"email":"c%d@x.com","username":"Case%d","displayName":"C","password":"password1"}
                """.formatted(n, n))).hasStatus(201);
        assertThat(post("/api/auth/register", """
                {"email":"d%d@x.com","username":"case%d","displayName":"D","password":"password1"}
                """.formatted(n, n))).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("USERNAME_TAKEN");
    }

    @Test
    void sessionDoesNotStorePasswordHash() {
        String email = register("password1", true);
        session(login(email, "password1"));
        String hash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
        var blobs = jdbc.queryForList("""
                select a.attribute_bytes from spring_session_attributes a
                join spring_session s on s.primary_id = a.session_primary_id where s.principal_name = ?
                """, byte[].class, email);
        assertThat(blobs).isNotEmpty()
                .allSatisfy(b -> assertThat(new String(b, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain(hash));
    }
}
