package com.recipehub.health;

import com.recipehub.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/api/health")).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void otherEndpointsRequireAuth() {
        assertThat(mvc.get().uri("/api/nope")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
