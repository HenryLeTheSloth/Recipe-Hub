package com.recipehub.recipe;

import com.recipehub.config.SecurityConfig;
import com.recipehub.recipe.dto.RecipeSearch;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(PublicRecipeController.class)
@Import(SecurityConfig.class)
class PublicRecipeControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RecipeQueryService service;

    @Test
    void searchIsPublic() {
        when(service.search(any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/recipes?q=trung&category=mon-chinh&ingredientIds=3,4&sort=newest").exchange())
                .hasStatusOk();
        var s = ArgumentCaptor.forClass(RecipeSearch.class);
        verify(service).search(s.capture(), any());
        assertThat(s.getValue()).isEqualTo(new RecipeSearch("trung", "mon-chinh", null, List.of(3L, 4L), "newest"));
    }

    @Test
    void sizeCappedAt50() {
        when(service.search(any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/recipes?size=500&page=2&sort=newest").exchange()).hasStatusOk();
        var p = ArgumentCaptor.forClass(Pageable.class);
        verify(service).search(any(), p.capture());
        assertThat(p.getValue().getPageSize()).isEqualTo(50);
        assertThat(p.getValue().getPageNumber()).isEqualTo(2);
        assertThat(p.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void detailIsPublic() {
        assertThat(mvc.get().uri("/api/recipes/trung-chien").exchange()).hasStatusOk();
        verify(service).getBySlug("trung-chien", null);
    }
}
