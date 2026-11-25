package com.recipehub.recipe;

import com.recipehub.catalog.Category;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientStatus;
import com.recipehub.catalog.Unit;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Máy trạng thái công thức — thuần Java, không Spring. */
class RecipeTransitionsTest {

    final User author = new User("a@x.com", "author", "HASH", "A");
    final User mod = new User("m@x.com", "mod", "HASH", "M");
    final Instant t1 = Instant.parse("2026-11-01T00:00:00Z");
    final Instant t2 = Instant.parse("2026-11-02T00:00:00Z");

    Recipe complete() {
        Recipe r = new Recipe(author, "Cà chua xào trứng", "ca-chua-xao-trung");
        r.setCategory(new Category("Món chính", "mon-chinh", 1));
        r.addIngredient(egg(), new BigDecimal("2"), Unit.PIECE, null);
        r.addStep("Đập trứng", null);
        return r;
    }

    static Ingredient egg() {
        return new Ingredient("Trứng gà", "trung-ga", IngredientGroup.EGG_DAIRY, "🥚", IngredientStatus.APPROVED, null);
    }

    /** Đưa công thức đủ dữ liệu tới trạng thái cần thử, chỉ qua các chuyển hợp lệ. */
    Recipe in(RecipeStatus s) {
        Recipe r = complete();
        switch (s) {
            case DRAFT -> { }
            case PENDING -> r.submit();
            case PUBLISHED -> { r.submit(); r.approve(mod, t1); }
            case REJECTED -> { r.submit(); r.reject(mod, "Thiếu ảnh", t1); }
            case HIDDEN -> { r.submit(); r.approve(mod, t1); r.hide(mod, "Spam", t2); }
        }
        assertThat(r.getStatus()).isEqualTo(s);
        return r;
    }

    static void assertInvalid(Executable e) {
        assertThatThrownBy(e::execute).isInstanceOfSatisfying(ApiException.class,
                x -> assertThat(x.code()).isEqualTo(ErrorCode.INVALID_TRANSITION));
    }

    @Test
    void newRecipeIsDraft() {
        assertThat(new Recipe(author, "Món", "mon").getStatus()).isEqualTo(RecipeStatus.DRAFT);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,PENDING", "REJECTED,PENDING"})
    void submitAllowed(RecipeStatus from, RecipeStatus to) {
        Recipe r = in(from);
        r.submit();
        assertThat(r.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest
    @EnumSource(names = {"PENDING", "PUBLISHED", "HIDDEN"})
    void submitDenied(RecipeStatus from) {
        Recipe r = in(from);
        assertInvalid(r::submit);
    }

    @Test
    void submitIncompleteListsMissing() {
        Recipe r = new Recipe(author, "Trứng luộc", "trung-luoc");
        r.addIngredient(egg(), BigDecimal.ONE, Unit.PIECE, null);
        assertThat(r.missingForSubmit()).containsExactly("danh mục", "các bước");
        assertThatThrownBy(r::submit).isInstanceOfSatisfying(ApiException.class, x -> {
            assertThat(x.code()).isEqualTo(ErrorCode.RECIPE_INCOMPLETE);
            assertThat(x.getMessage()).isEqualTo("Còn thiếu: danh mục, các bước");
        });
        assertThat(r.getStatus()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(new Recipe(author, "Rỗng", "rong").missingForSubmit())
                .containsExactly("danh mục", "nguyên liệu", "các bước");
    }

    @Test
    void editingPublishedGoesBackToPending() {
        Recipe r = in(RecipeStatus.PUBLISHED);
        r.markEdited();
        assertThat(r.getStatus()).isEqualTo(RecipeStatus.PENDING);
    }

    @ParameterizedTest
    @EnumSource(names = {"DRAFT", "REJECTED"})
    void editingDraftOrRejectedKeepsStatus(RecipeStatus s) {
        Recipe r = in(s);
        r.markEdited();
        assertThat(r.getStatus()).isEqualTo(s);
    }

    @ParameterizedTest
    @EnumSource(names = {"PENDING", "HIDDEN"})
    void editingDenied(RecipeStatus s) {
        Recipe r = in(s);
        assertInvalid(r::markEdited);
    }

    @Test
    void approveKeepsOriginalPublishedAt() {
        Recipe r = in(RecipeStatus.PUBLISHED);
        r.markEdited();
        r.approve(mod, t2);
        assertThat(r.getPublishedAt()).isEqualTo(t1);
        assertThat(r.getReviewedAt()).isEqualTo(t2);
        assertThat(r.getReviewedBy()).isSameAs(mod);
    }

    @Test
    void approveClearsRejectionReason() {
        Recipe r = in(RecipeStatus.REJECTED);
        assertThat(r.getRejectionReason()).isEqualTo("Thiếu ảnh");
        r.submit();
        r.approve(mod, t2);
        assertThat(r.getRejectionReason()).isNull();
        assertThat(r.getPublishedAt()).isEqualTo(t2);
    }

    @ParameterizedTest
    @EnumSource(names = {"DRAFT", "PUBLISHED", "REJECTED", "HIDDEN"})
    void approveOnlyFromPending(RecipeStatus s) {
        Recipe r = in(s);
        assertInvalid(() -> r.approve(mod, t2));
        assertInvalid(() -> r.reject(mod, "x", t2));
    }

    @ParameterizedTest
    @EnumSource(names = {"DRAFT", "PENDING", "REJECTED", "HIDDEN"})
    void hideOnlyFromPublished(RecipeStatus s) {
        Recipe r = in(s);
        assertInvalid(() -> r.hide(mod, "x", t2));
    }

    @Test
    void hideStoresReasonAndUnhideRestores() {
        Recipe r = in(RecipeStatus.HIDDEN);
        assertThat(r.getRejectionReason()).isEqualTo("Spam");
        r.unhide(mod, t2);
        assertThat(r.getStatus()).isEqualTo(RecipeStatus.PUBLISHED);
        assertThat(r.getRejectionReason()).isNull();
        assertThat(r.getPublishedAt()).isEqualTo(t1);
    }

    @ParameterizedTest
    @EnumSource(names = {"DRAFT", "PENDING", "PUBLISHED", "REJECTED"})
    void unhideOnlyFromHidden(RecipeStatus s) {
        Recipe r = in(s);
        assertInvalid(() -> r.unhide(mod, t2));
    }

    @Test
    void childrenGetPositionAndStepNumbers() {
        Recipe r = complete();
        r.addStep("Xào", null);
        assertThat(r.getSteps()).extracting(RecipeStep::getStepNo).containsExactly(1, 2);
        assertThat(r.getIngredients()).extracting(RecipeIngredient::getPosition).containsExactly(0);
    }
}
