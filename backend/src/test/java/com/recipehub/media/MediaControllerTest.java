package com.recipehub.media;

import com.recipehub.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(MediaController.class)
@Import(SecurityConfig.class)
class MediaControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean MediaService media;
    @MockitoBean StorageService storage;

    final MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

    @Test
    void uploadRequiresAuth() {
        assertThat(mvc.post().uri("/api/media").multipart().file(file).with(csrf()).exchange()).hasStatus(401);
    }

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void uploadRequiresMediaUpload() {
        assertThat(mvc.post().uri("/api/media").multipart().file(file).with(csrf()).exchange()).hasStatus(403);
    }
}
