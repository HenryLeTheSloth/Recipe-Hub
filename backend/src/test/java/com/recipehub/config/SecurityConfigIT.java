package com.recipehub.config;

import com.recipehub.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

// Server thật (không MockMvc) vì lỗi nằm ở error dispatch của Tomcat.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SecurityConfigIT {

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> send(String method, String path) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void postWithoutCsrfIs403NotRewrittenTo401() throws Exception {
        assertThat(send("POST", "/api/health").statusCode()).isEqualTo(403);
    }

    @Test
    void anonymous401DoesNotCreateSession() throws Exception {
        var res = send("GET", "/api/nope");
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.headers().allValues("Set-Cookie")).noneMatch(c -> c.startsWith("SESSION="));
    }

    @Test
    void anonymous401IsProblemDetailWithCode() throws Exception {
        var res = send("GET", "/api/me");
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.headers().firstValue("Content-Type")).hasValueSatisfying(t -> assertThat(t).contains("problem+json"));
        assertThat(res.body()).contains("\"code\":\"UNAUTHENTICATED\"");
    }

    // MockMvc + csrf() thay repository của CsrfFilter, nên cookie thật chỉ kiểm được qua server thật.
    @Test
    void csrfEndpointSetsXsrfCookie() throws Exception {
        var res = send("GET", "/api/auth/csrf");
        assertThat(res.statusCode()).isEqualTo(204);
        assertThat(res.headers().allValues("Set-Cookie")).anyMatch(c -> c.startsWith("XSRF-TOKEN="));
    }
}
