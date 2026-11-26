package com.recipehub.community;

import com.recipehub.community.dto.CommentView;
import com.recipehub.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({CommentController.class, AdminCommentController.class, FavoriteController.class})
@Import(SecurityConfig.class)
class CommunityControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean CommentService comments;
    @MockitoBean FavoriteService favorites;

    static final CommentView VIEW = new CommentView(1L, "hi", null, false, Instant.EPOCH, List.of());

    @Test
    void anonymousCanReadComments() {
        when(comments.list(eq(100L), any())).thenReturn(List.of(VIEW));
        assertThat(mvc.get().uri("/api/recipes/100/comments").exchange()).hasStatusOk();
    }

    @Test
    void anonymousCannotPostOrFavorite() {
        assertThat(mvc.post().uri("/api/recipes/100/comments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"x\"}").exchange()).hasStatus(401);
        assertThat(mvc.get().uri("/api/me/favorites").exchange()).hasStatus(401);
    }

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void commentingNeedsCommunityPermission() {
        assertThat(mvc.post().uri("/api/recipes/100/comments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"x\"}").exchange()).hasStatus(403);
        assertThat(mvc.put().uri("/api/recipes/100/favorite").with(csrf()).exchange()).hasStatus(403);
        assertThat(mvc.patch().uri("/api/admin/comments/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"hidden\":true}").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "COMMUNITY_PARTICIPATE")
    void memberCanCommentValidatedAndFavorite() {
        when(comments.create(any(), eq(100L), any())).thenReturn(VIEW);
        when(favorites.listMine(any(), any())).thenReturn(Page.empty());
        assertThat(mvc.post().uri("/api/recipes/100/comments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"  \"}").exchange()).hasStatus(400);
        assertThat(mvc.post().uri("/api/recipes/100/comments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hi\"}").exchange()).hasStatus(201);
        assertThat(mvc.put().uri("/api/recipes/100/favorite").with(csrf()).exchange()).hasStatus(204);
        assertThat(mvc.delete().uri("/api/recipes/100/favorite").with(csrf()).exchange()).hasStatus(204);
        assertThat(mvc.delete().uri("/api/comments/1").with(csrf()).exchange()).hasStatus(204);
    }

    @Test
    @WithMockUser(authorities = "CONTENT_MODERATE")
    void moderatorCanHide() {
        when(comments.setHidden(1L, true)).thenReturn(VIEW);
        assertThat(mvc.patch().uri("/api/admin/comments/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"hidden\":true}").exchange()).hasStatusOk();
    }
}
