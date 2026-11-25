package com.recipehub.media;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.user.MailService;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "app.storage.dir=target/test-uploads")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MediaIT {

    static final AtomicInteger SEQ = new AtomicInteger();
    static final byte[] PNG = MediaTypesTest.hex("89504E470D0A1A0A0000000D49484452");

    @Autowired MockMvcTester mvc;
    @MockitoBean MailService mail;

    MvcTestResult json(String method, String path, String body, Cookie s) {
        var req = mvc.method(org.springframework.http.HttpMethod.valueOf(method)).uri(path).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (s != null) req = req.cookie(s);
        return req.exchange();
    }

    /** Member mới đã xác minh, đã đăng nhập. */
    Cookie member() {
        int n = SEQ.incrementAndGet();
        String email = "media" + n + "@x.com";
        assertThat(json("POST", "/api/auth/register", """
                {"email":"%s","username":"media%d","displayName":"M","password":"password1"}
                """.formatted(email, n), null)).hasStatus(201);
        var token = ArgumentCaptor.forClass(String.class);
        verify(mail).sendVerification(eq(email), token.capture());
        assertThat(json("POST", "/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token.getValue()), null))
                .hasStatus(204);
        var login = json("POST", "/api/auth/login", "{\"email\":\"%s\",\"password\":\"password1\"}".formatted(email), null);
        assertThat(login).hasStatusOk();
        return login.getResponse().getCookie("SESSION");
    }

    MvcTestResult upload(byte[] bytes, Cookie s) {
        return mvc.post().uri("/api/media").multipart().file(new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes))
                .with(csrf()).cookie(s).exchange();
    }

    Number id(MvcTestResult r) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void uploadServeAndSetAvatar() throws Exception {
        Cookie s = member();
        var up = upload(PNG, s);
        assertThat(up).hasStatus(201).bodyJson().extractingPath("$.url").asString().matches("/api/media/\\d+/file");
        Number id = id(up);

        var file = mvc.get().uri("/api/media/" + id + "/file").exchange(); // công khai
        assertThat(file).hasStatusOk().hasContentType("image/png").hasHeader("Cache-Control", "max-age=86400");
        assertThat(file.getResponse().getContentAsByteArray()).containsExactly(PNG);

        assertThat(json("PUT", "/api/me/avatar", "{\"mediaId\":" + id + "}", s)).hasStatusOk()
                .bodyJson().extractingPath("$.avatarUrl").isEqualTo("/api/media/" + id + "/file");
        assertThat(mvc.get().uri("/api/me").cookie(s).exchange()).bodyJson()
                .extractingPath("$.avatarUrl").isEqualTo("/api/media/" + id + "/file");
    }

    @Test
    void htmlDisguisedAsJpgIs415() {
        assertThat(upload("<html><script>alert(1)</script></html>".getBytes(), member())).hasStatus(415)
                .bodyJson().extractingPath("$.code").isEqualTo("MEDIA_TYPE");
    }

    @Test
    void sixMegabytesIs413Json() {
        byte[] big = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThat(upload(big, member())).hasStatus(413)
                .bodyJson().extractingPath("$.code").isEqualTo("MEDIA_TOO_LARGE");
    }

    @Test
    void avatarWithOthersMediaIs403() throws Exception {
        Number othersId = id(upload(PNG, member()));
        assertThat(json("PUT", "/api/me/avatar", "{\"mediaId\":" + othersId + "}", member())).hasStatus(403)
                .bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void unknownMediaFileIs404() {
        assertThat(mvc.get().uri("/api/media/999999/file").exchange()).hasStatus(404);
    }
}
