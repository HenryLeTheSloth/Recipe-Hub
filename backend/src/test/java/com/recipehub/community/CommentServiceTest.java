package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.community.dto.CommentRequest;
import com.recipehub.community.dto.CommentView;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeMapper;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository comments;
    @Mock RecipeRepository recipes;
    @Mock UserRepository users;
    CommentService service;

    final User cook = withId(new User("a@x.com", "cook", "HASH", "Cook"), 1L);
    final User member = withId(new User("m@x.com", "member", "HASH", "Member"), 7L);
    final Recipe recipe = withId(new Recipe(cook, "Cà chua xào trứng", "ca-chua-xao-trung"), 100L);
    final Recipe other = withId(new Recipe(cook, "Món khác", "mon-khac"), 200L);

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, recipes, users, Mappers.getMapper(RecipeMapper.class));
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.PUBLISHED);
        lenient().when(recipes.findById(100L)).thenReturn(Optional.of(recipe));
        lenient().when(users.getReferenceById(7L)).thenReturn(member);
        lenient().when(comments.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static AppUserDetails actor(long id, String... perms) {
        return new AppUserDetails(id, "u" + id + "@x.com", "HASH", true, false, Set.of(perms));
    }

    static void assertCode(Runnable r, ErrorCode code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    void replyToReplyRejected() {
        Comment root = withId(new Comment(recipe, cook, null, "gốc"), 10L);
        Comment reply = withId(new Comment(recipe, cook, root, "trả lời"), 11L);
        when(comments.findById(11L)).thenReturn(Optional.of(reply));
        assertCode(() -> service.create(actor(7L), 100L, new CommentRequest("x", 11L)), ErrorCode.COMMENT_DEPTH);
        verify(comments, never()).save(any());
    }

    @Test
    void replyToRootCreatesReply() {
        Comment root = withId(new Comment(recipe, cook, null, "gốc"), 10L);
        when(comments.findById(10L)).thenReturn(Optional.of(root));
        CommentView v = service.create(actor(7L), 100L, new CommentRequest("ok", 10L));
        assertThat(v.content()).isEqualTo("ok");
        assertThat(v.author().username()).isEqualTo("member");
        assertThat(v.replies()).isEmpty();
    }

    @Test
    void parentMustBelongToSameRecipe() {
        Comment foreign = withId(new Comment(other, cook, null, "gốc"), 20L);
        when(comments.findById(20L)).thenReturn(Optional.of(foreign));
        assertCode(() -> service.create(actor(7L), 100L, new CommentRequest("x", 20L)), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void cannotCommentOnUnpublished() {
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.HIDDEN);
        assertCode(() -> service.create(actor(7L), 100L, new CommentRequest("x", null)), ErrorCode.NOT_FOUND);
        assertCode(() -> service.list(100L, null), ErrorCode.NOT_FOUND);
    }

    @Test
    void hiddenInvisibleToMembersVisibleToModerators() {
        Comment g1 = withId(new Comment(recipe, cook, null, "G1"), 10L);
        g1.setHidden(true);
        Comment g2 = withId(new Comment(recipe, cook, null, "G2"), 12L);
        Comment r1 = withId(new Comment(recipe, cook, g1, "R1"), 11L);
        when(comments.findTop200ByRecipe_IdAndParentIsNullOrderByCreatedAtAscIdAsc(100L)).thenReturn(List.of(g1, g2));
        when(comments.findByParent_IdInOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of(r1));

        List<CommentView> forMember = service.list(100L, actor(7L, "COMMUNITY_PARTICIPATE"));
        assertThat(forMember).extracting(CommentView::content).containsExactly("G2");

        List<CommentView> forMod = service.list(100L, actor(9L, "CONTENT_MODERATE"));
        assertThat(forMod).extracting(CommentView::content).containsExactly("G1", "G2");
        assertThat(forMod.getFirst().hidden()).isTrue();
        assertThat(forMod.getFirst().replies()).extracting(CommentView::content).containsExactly("R1");
        assertThat(forMod.get(1).replies()).isEmpty();
    }

    @Test
    void othersCommentCannotBeDeletedByMember() {
        Comment c = withId(new Comment(recipe, cook, null, "gốc"), 10L);
        when(comments.findById(10L)).thenReturn(Optional.of(c));
        assertCode(() -> service.delete(actor(7L, "COMMUNITY_PARTICIPATE"), 10L), ErrorCode.FORBIDDEN);
        verify(comments, never()).delete(any());
    }

    @Test
    void authorAndModeratorCanDelete() {
        Comment c = withId(new Comment(recipe, member, null, "của tôi"), 10L);
        when(comments.findById(10L)).thenReturn(Optional.of(c));
        service.delete(actor(7L), 10L);
        service.delete(actor(9L, "CONTENT_MODERATE"), 10L);
        verify(comments, org.mockito.Mockito.times(2)).delete(c);
    }

    @Test
    void deleteUnknownIsNotFound() {
        assertCode(() -> service.delete(actor(7L), 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void setHiddenTogglesFlag() {
        Comment c = withId(new Comment(recipe, cook, null, "gốc"), 10L);
        when(comments.findById(10L)).thenReturn(Optional.of(c));
        assertThat(service.setHidden(10L, true).hidden()).isTrue();
    }
}
