package com.recipehub.recipe;

import com.recipehub.catalog.Category;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientStatus;
import com.recipehub.catalog.Unit;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeModerationServiceTest {

    @Mock RecipeRepository recipes;
    @Mock UserRepository users;
    RecipeModerationService service;

    final Instant now = Instant.parse("2026-11-20T03:00:00Z");
    final User modUser = withId(new User("m@x.com", "mod", "HASH", "Mod"), 9L);
    final AppUserDetails mod = new AppUserDetails(9L, "m@x.com", "HASH", true, false, Set.of("CONTENT_MODERATE"));

    @BeforeEach
    void setUp() {
        service = new RecipeModerationService(recipes, users, Mappers.getMapper(RecipeMapper.class),
                Clock.fixed(now, ZoneOffset.UTC));
        lenient().when(users.getReferenceById(9L)).thenReturn(modUser);
    }

    static <T> T withId(T e, long id) {
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    Recipe stored(boolean submitted) {
        Recipe r = withId(new Recipe(withId(new User("a@x.com", "author", "HASH", "A"), 1L), "Trứng chiên", "trung-chien"), 100L);
        r.setCategory(new Category("Món chính", "mon-chinh", 1));
        r.addIngredient(new Ingredient("Trứng gà", "trung-ga", IngredientGroup.EGG_DAIRY, "🥚", IngredientStatus.APPROVED, null),
                BigDecimal.ONE, Unit.PIECE, null);
        r.addStep("Chiên", null);
        if (submitted) r.submit();
        when(recipes.findById(100L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void approveUsesClockAndModerator() {
        Recipe r = stored(true);
        var d = service.approve(mod, 100L);
        assertThat(d.status()).isEqualTo(RecipeStatus.PUBLISHED);
        assertThat(d.publishedAt()).isEqualTo(now);
        assertThat(r.getReviewedAt()).isEqualTo(now);
        assertThat(r.getReviewedBy()).isSameAs(modUser);
    }

    @Test
    void approveNonPendingIs409() {
        stored(false);
        assertThatThrownBy(() -> service.approve(mod, 100L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_TRANSITION));
    }

    @Test
    void rejectStoresReason() {
        stored(true);
        var d = service.reject(mod, 100L, "  Thiếu ảnh  ");
        assertThat(d.status()).isEqualTo(RecipeStatus.REJECTED);
        assertThat(d.rejectionReason()).isEqualTo("Thiếu ảnh");
    }

    @Test
    void hideThenUnhide() {
        stored(true);
        service.approve(mod, 100L);
        assertThat(service.hide(mod, 100L, "Spam").status()).isEqualTo(RecipeStatus.HIDDEN);
        assertThat(service.unhide(mod, 100L).status()).isEqualTo(RecipeStatus.PUBLISHED);
    }

    @Test
    void missingRecipeIs404() {
        when(recipes.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void queueOldestFirst() {
        var expected = PageRequest.of(2, 10, Sort.by(Sort.Direction.ASC, "updatedAt"));
        when(recipes.findByStatus(RecipeStatus.PENDING, expected)).thenReturn(Page.empty());
        service.queue(null, PageRequest.of(2, 10));
        verify(recipes).findByStatus(RecipeStatus.PENDING, expected);
    }
}
