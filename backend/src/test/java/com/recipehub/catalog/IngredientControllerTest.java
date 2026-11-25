package com.recipehub.catalog;

import com.recipehub.catalog.dto.IngredientResponse;
import com.recipehub.catalog.dto.SuggestResult;
import com.recipehub.config.SecurityConfig;
import com.recipehub.user.AppUserDetails;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({IngredientController.class, AdminIngredientController.class})
@Import(SecurityConfig.class)
class IngredientControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean IngredientService service;

    static final AppUserDetails MEMBER = new AppUserDetails(7L, "m@x.com", null, true, false, Set.of("RECIPE_WRITE"));
    static final IngredientResponse TOMATO =
            new IngredientResponse(1L, "Cà chua", "ca-chua", IngredientGroup.VEGETABLE, "🍅", IngredientStatus.APPROVED);

    @Test
    void listIsPublic() {
        when(service.listApproved("ca", IngredientGroup.VEGETABLE)).thenReturn(List.of(TOMATO));
        assertThat(mvc.get().uri("/api/ingredients?q=ca&group=VEGETABLE").exchange()).hasStatusOk()
                .bodyJson().extractingPath("$[0].slug").isEqualTo("ca-chua");
    }

    @Test
    void suggestStatusCodes() {
        String body = "{\"name\":\"Cà chua\",\"group\":\"VEGETABLE\"}";
        when(service.suggest(eq(7L), any())).thenReturn(new SuggestResult(TOMATO, true));
        assertThat(mvc.post().uri("/api/ingredients").with(csrf()).with(user(MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatus(201)
                .bodyJson().extractingPath("$.slug").isEqualTo("ca-chua");
        when(service.suggest(eq(7L), any())).thenReturn(new SuggestResult(TOMATO, false));
        assertThat(mvc.post().uri("/api/ingredients").with(csrf()).with(user(MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatus(200);
    }

    @Test
    void suggestRequiresLoginAndGroup() {
        assertThat(mvc.post().uri("/api/ingredients").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"group\":\"VEGETABLE\"}").exchange()).hasStatus(401);
        assertThat(mvc.post().uri("/api/ingredients").with(csrf()).with(user(MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\"}").exchange()).hasStatus(400);
    }

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void memberCannotApprove() {
        assertThat(mvc.put().uri("/api/admin/ingredients/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"group\":\"VEGETABLE\",\"status\":\"APPROVED\"}").exchange()).hasStatus(403);
        assertThat(mvc.get().uri("/api/admin/ingredients").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "CATALOG_MANAGE")
    void adminUpdateNeedsStatus() {
        assertThat(mvc.put().uri("/api/admin/ingredients/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"group\":\"VEGETABLE\"}").exchange()).hasStatus(400);
    }
}
