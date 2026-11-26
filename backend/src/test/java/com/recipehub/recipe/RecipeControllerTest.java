package com.recipehub.recipe;

import com.recipehub.config.SecurityConfig;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.user.AppUserDetails;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({RecipeController.class, MyRecipeController.class})
@Import(SecurityConfig.class)
class RecipeControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RecipeService service;

    static final AppUserDetails MEMBER = new AppUserDetails(7L, "m@x.com", null, true, false, Set.of("RECIPE_WRITE"));
    static final String VALID = """
            {"title":"Trứng chiên","servings":2,"prepMinutes":5,"cookMinutes":5,"difficulty":"EASY",
             "tagIds":[],"ingredients":[{"ingredientId":5,"quantity":2,"unit":"PIECE"}],"steps":[{"content":"Chiên"}]}
            """;
    static final RecipeDetail DETAIL = new RecipeDetail(100L, "trung-chien", "Trứng chiên", null, null, List.of(), 2, 5, 5,
            Difficulty.EASY, RecipeStatus.DRAFT, null, null, null, List.of(), List.of(), null, null, Instant.EPOCH, Instant.EPOCH, null, 0, false);

    @Test
    @WithMockUser(authorities = "COMMUNITY_PARTICIPATE")
    void createRequiresRecipeWrite() {
        assertThat(mvc.post().uri("/api/recipes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID)
                .exchange()).hasStatus(403);
    }

    @Test
    void createReturns201() {
        when(service.create(eq(MEMBER), any())).thenReturn(DETAIL);
        assertThat(mvc.post().uri("/api/recipes").with(csrf()).with(user(MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(VALID).exchange()).hasStatus(201)
                .bodyJson().extractingPath("$.slug").isEqualTo("trung-chien");
    }

    @Test
    void validationErrors() {
        String bad = VALID.replace("\"Trứng chiên\"", "\"ab\"").replace("\"servings\":2", "\"servings\":0")
                .replace("\"quantity\":2", "\"quantity\":0.001");
        var res = mvc.post().uri("/api/recipes").with(csrf()).with(user(MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(bad).exchange();
        assertThat(res).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
        assertThat(res).bodyJson().extractingPath("$.errors").asMap()
                .containsKeys("title", "servings", "ingredients[0].quantity");
    }

    @Test
    void deleteNeedsOnlyLogin() {
        var viewer = new AppUserDetails(8L, "v@x.com", null, true, false, Set.of());
        assertThat(mvc.delete().uri("/api/recipes/100").with(csrf()).with(user(viewer)).exchange()).hasStatus(204);
        verify(service).delete(viewer, 100L);
        assertThat(mvc.delete().uri("/api/recipes/100").with(csrf()).exchange()).hasStatus(401);
    }

    @Test
    void myRecipesFilterByStatus() {
        when(service.listMine(any(), any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/me/recipes?status=DRAFT,PENDING&page=1&size=5").with(user(MEMBER)).exchange())
                .hasStatusOk().bodyJson().extractingPath("$.page.size").isEqualTo(0);
        verify(service).listMine(MEMBER, Set.of(RecipeStatus.DRAFT, RecipeStatus.PENDING), PageRequest.of(1, 5));
    }
}
