package com.recipehub.catalog;

import com.recipehub.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({CatalogController.class, AdminCatalogController.class})
@Import(SecurityConfig.class)
class CatalogControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean CategoryService categories;
    @MockitoBean TagService tags;

    @Test
    void categoriesAndTagsArePublic() {
        when(categories.list()).thenReturn(List.of());
        when(tags.list()).thenReturn(List.of());
        assertThat(mvc.get().uri("/api/categories").exchange()).hasStatusOk();
        assertThat(mvc.get().uri("/api/tags").exchange()).hasStatusOk();
    }

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void memberCannotCreateCategoryOrTag() {
        assertThat(mvc.post().uri("/api/admin/categories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"sortOrder\":0}").exchange()).hasStatus(403);
        assertThat(mvc.post().uri("/api/admin/tags").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\"}").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "CATALOG_MANAGE")
    void blankNameIs400() {
        assertThat(mvc.post().uri("/api/admin/tags").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \"}").exchange()).hasStatus(400);
    }
}
