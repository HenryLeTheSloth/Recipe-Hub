package com.recipehub.recipe;

import com.recipehub.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AdminRecipeController.class)
@Import(SecurityConfig.class)
class AdminRecipeControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RecipeModerationService service;

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void memberCannotModerate() {
        assertThat(mvc.get().uri("/api/admin/recipes").exchange()).hasStatus(403);
        assertThat(mvc.post().uri("/api/admin/recipes/1/approve").with(csrf()).exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "CONTENT_MODERATE")
    void blankReasonIs400() {
        assertThat(mvc.post().uri("/api/admin/recipes/1/reject").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\" \"}").exchange()).hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
        assertThat(mvc.post().uri("/api/admin/recipes/1/hide").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}").exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(authorities = "CONTENT_MODERATE")
    void moderatorCanListAndApprove() {
        when(service.queue(any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/admin/recipes?status=HIDDEN").exchange()).hasStatusOk();
        assertThat(mvc.post().uri("/api/admin/recipes/1/approve").with(csrf()).exchange()).hasStatusOk();
    }
}
