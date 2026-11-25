package com.recipehub.catalog;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.user.MailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogIT {

    @Autowired MockMvcTester mvc;
    @MockitoBean MailService mail;

    MvcTestResult send(String method, String path, String json, Cookie s) {
        var req = mvc.method(HttpMethod.valueOf(method)).uri(path).with(csrf());
        if (s != null) req = req.cookie(s);
        if (json != null) req = req.contentType(MediaType.APPLICATION_JSON).content(json);
        return req.exchange();
    }

    Cookie admin() {
        var r = send("POST", "/api/auth/login", "{\"email\":\"root@test.local\",\"password\":\"RootPass123\"}", null);
        assertThat(r).hasStatusOk();
        return r.getResponse().getCookie("SESSION");
    }

    @Test
    void tagSlugCollisionAndCaseInsensitiveName() {
        Cookie a = admin();
        assertThat(send("POST", "/api/admin/tags", "{\"name\":\"Bánh ngọt\"}", a)).hasStatus(201)
                .bodyJson().extractingPath("$.slug").isEqualTo("banh-ngot");
        assertThat(send("POST", "/api/admin/tags", "{\"name\":\"Banh ngot\"}", a)).hasStatus(201)
                .bodyJson().extractingPath("$.slug").isEqualTo("banh-ngot-2");
        assertThat(send("POST", "/api/admin/tags", "{\"name\":\"BÁNH NGỌT\"}", a)).hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("NAME_TAKEN");
        assertThat(send("GET", "/api/tags", null, null)).hasStatusOk()
                .bodyJson().extractingPath("$[*].slug").asArray().contains("banh-ngot", "banh-ngot-2");
    }

    @Test
    void categoryCrudAndOrdering() throws Exception {
        Cookie a = admin();
        var z = send("POST", "/api/admin/categories", "{\"name\":\"Zzz cuối\",\"sortOrder\":-1}", a);
        assertThat(z).hasStatus(201);
        Number id = com.jayway.jsonpath.JsonPath.read(z.getResponse().getContentAsString(), "$.id");
        assertThat(send("GET", "/api/categories", null, null)).hasStatusOk()
                .bodyJson().extractingPath("$[0].name").isEqualTo("Zzz cuối");
        assertThat(send("PUT", "/api/admin/categories/" + id, "{\"name\":\"Đầu tiên\",\"sortOrder\":-1}", a))
                .hasStatusOk().bodyJson().extractingPath("$.slug").isEqualTo("dau-tien");
        assertThat(send("DELETE", "/api/admin/categories/" + id, null, a)).hasStatus(204);
        assertThat(send("DELETE", "/api/admin/categories/" + id, null, a)).hasStatus(404);
    }
}
