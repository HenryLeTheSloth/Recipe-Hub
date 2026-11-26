package com.recipehub.community;

import com.recipehub.config.SecurityConfig;
import com.recipehub.community.dto.RatingView;
import com.recipehub.user.AppUserDetails;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({RatingController.class, AdminRatingController.class})
@Import(SecurityConfig.class)
class RatingControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean RatingService service;

    static AppUserDetails with(String... perms) {
        return new AppUserDetails(7L, "m@x.com", null, true, false, Set.of(perms));
    }

    static final RatingView VIEW = new RatingView(1L, 5, null, null, false, Instant.EPOCH);

    @Test
    void listIsPublic() {
        when(service.list(eq(100L), any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/recipes/100/ratings").exchange()).hasStatusOk();
    }

    @Test
    void rateNeedsLoginAndPermission() {
        String body = "{\"stars\":5}";
        assertThat(mvc.put().uri("/api/recipes/100/my-rating").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange()).hasStatus(401);
        assertThat(mvc.put().uri("/api/recipes/100/my-rating").with(csrf()).with(user(with("RECIPE_WRITE")))
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatus(403);
    }

    @Test
    void rateValidatesStarsAndReturnsView() {
        var member = with("COMMUNITY_PARTICIPATE");
        when(service.upsert(eq(member), eq(100L), any())).thenReturn(VIEW);
        assertThat(mvc.put().uri("/api/recipes/100/my-rating").with(csrf()).with(user(member))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stars\":6}").exchange()).hasStatus(400);
        assertThat(mvc.put().uri("/api/recipes/100/my-rating").with(csrf()).with(user(member))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stars\":5}").exchange()).hasStatusOk();
        assertThat(mvc.delete().uri("/api/recipes/100/my-rating").with(csrf()).with(user(member)).exchange())
                .hasStatus(204);
    }

    @Test
    void hidingNeedsModerate() {
        when(service.setHidden(1L, true)).thenReturn(VIEW);
        String body = "{\"hidden\":true}";
        assertThat(mvc.patch().uri("/api/admin/ratings/1").with(csrf()).with(user(with("COMMUNITY_PARTICIPATE")))
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatus(403);
        assertThat(mvc.patch().uri("/api/admin/ratings/1").with(csrf()).with(user(with("CONTENT_MODERATE")))
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatusOk();
    }
}
