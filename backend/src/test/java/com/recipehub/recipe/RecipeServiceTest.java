package com.recipehub.recipe;

import com.recipehub.catalog.Category;
import com.recipehub.catalog.CategoryRepository;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientRepository;
import com.recipehub.catalog.IngredientStatus;
import com.recipehub.catalog.TagRepository;
import com.recipehub.catalog.Unit;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.media.MediaService;
import com.recipehub.recipe.dto.RecipeIngredientRequest;
import com.recipehub.recipe.dto.RecipeRequest;
import com.recipehub.recipe.dto.RecipeStepRequest;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
class RecipeServiceTest {

    @Mock RecipeRepository recipes;
    @Mock CategoryRepository categories;
    @Mock TagRepository tags;
    @Mock IngredientRepository ingredients;
    @Mock UserRepository users;
    @Mock MediaService media;
    RecipeService service;

    final User author = withId(new User("a@x.com", "author", "HASH", "Tác giả"), 1L);
    final AppUserDetails authorActor = actor(1L, "RECIPE_WRITE");
    final AppUserDetails other = actor(7L, "RECIPE_WRITE");
    final Ingredient egg = withId(new Ingredient("Trứng gà", "trung-ga", IngredientGroup.EGG_DAIRY, "🥚",
            IngredientStatus.APPROVED, null), 5L);

    @BeforeEach
    void setUp() {
        service = new RecipeService(recipes, categories, tags, ingredients, users, media,
                Mappers.getMapper(RecipeMapper.class));
        lenient().when(users.getReferenceById(1L)).thenReturn(author);
        lenient().when(tags.findAllById(any())).thenReturn(List.of());
        lenient().when(ingredients.findAllById(any())).thenReturn(List.of(egg));
        lenient().when(recipes.save(any())).thenAnswer(i -> withId(i.getArgument(0), 100L));
    }

    static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static AppUserDetails actor(long id, String... perms) {
        return new AppUserDetails(id, "u" + id + "@x.com", "HASH", true, false, Set.of(perms));
    }

    static RecipeRequest req(String title, Long coverId, List<RecipeIngredientRequest> lines) {
        return new RecipeRequest(title, null, null, 2, 5, 10, Difficulty.EASY, coverId, Set.of(), lines,
                List.of(new RecipeStepRequest("Đập trứng", null)));
    }

    static RecipeIngredientRequest line(long ingredientId) {
        return new RecipeIngredientRequest(ingredientId, new BigDecimal("2"), Unit.PIECE, null);
    }

    Recipe stored(RecipeStatus status) {
        Recipe r = withId(new Recipe(author, "Cà chua xào trứng", "ca-chua-xao-trung"), 100L);
        r.setCategory(new Category("Món chính", "mon-chinh", 1));
        r.addIngredient(egg, BigDecimal.ONE, Unit.PIECE, null);
        r.addStep("Xào", null);
        User mod = new User("m@x.com", "mod", "HASH", "M");
        switch (status) {
            case DRAFT -> { }
            case PUBLISHED -> { r.submit(); r.approve(mod, Instant.EPOCH); }
            default -> throw new IllegalArgumentException(status.name());
        }
        when(recipes.findById(100L)).thenReturn(Optional.of(r));
        return r;
    }

    static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    @Test
    void createStartsAsDraftWithUniqueSlug() {
        when(recipes.existsBySlug("ca-chua-xao-trung")).thenReturn(true);
        var d = service.create(authorActor, req("Cà chua xào trứng", null, List.of(line(5))));
        assertThat(d.slug()).isEqualTo("ca-chua-xao-trung-2");
        assertThat(d.status()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(d.author().username()).isEqualTo("author");
        assertThat(d.ingredients()).extracting(l -> l.name()).containsExactly("Trứng gà");
        assertThat(d.steps()).extracting(s -> s.stepNo()).containsExactly(1);
    }

    @Test
    void duplicateIngredientRejected() {
        assertCode(() -> service.create(authorActor, req("Trứng chiên", null, List.of(line(5), line(5)))),
                ErrorCode.DUPLICATE_INGREDIENT);
        verify(recipes, never()).save(any());
    }

    @Test
    void unknownIngredientIsValidationError() {
        assertCode(() -> service.create(authorActor, req("Trứng chiên", null, List.of(line(5), line(6)))),
                ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void othersDraftIsNotFound() {
        stored(RecipeStatus.DRAFT);
        assertCode(() -> service.update(other, 100L, req("Món mới", null, List.of(line(5)))), ErrorCode.NOT_FOUND);
        assertCode(() -> service.submit(other, 100L), ErrorCode.NOT_FOUND);
        assertCode(() -> service.delete(other, 100L), ErrorCode.NOT_FOUND);
        assertCode(() -> service.getMine(other, 100L), ErrorCode.NOT_FOUND);
    }

    @Test
    void othersPublishedIsForbiddenToEdit() {
        stored(RecipeStatus.PUBLISHED);
        assertCode(() -> service.update(other, 100L, req("Món mới", null, List.of(line(5)))), ErrorCode.FORBIDDEN);
        assertCode(() -> service.submit(other, 100L), ErrorCode.FORBIDDEN);
        assertCode(() -> service.delete(other, 100L), ErrorCode.FORBIDDEN);
    }

    @Test
    void moderatorCanDeleteOthersRecipe() {
        Recipe r = stored(RecipeStatus.DRAFT);
        service.delete(actor(9L, "CONTENT_MODERATE"), 100L);
        verify(recipes).delete(r);
    }

    @Test
    void moderatorCannotEditOthersRecipe() {
        stored(RecipeStatus.DRAFT);
        assertCode(() -> service.update(actor(9L, "RECIPE_WRITE", "CONTENT_MODERATE"), 100L,
                req("Món mới", null, List.of(line(5)))), ErrorCode.FORBIDDEN);
    }

    @Test
    void coverMustBeOwned() {
        when(media.requireOwned(55L, 1L)).thenThrow(new ApiException(ErrorCode.FORBIDDEN, "Ảnh không thuộc về bạn"));
        assertCode(() -> service.create(authorActor, req("Trứng chiên", 55L, List.of(line(5)))), ErrorCode.FORBIDDEN);
        verify(recipes, never()).save(any());
    }

    @Test
    void slugDoesNotChangeOnTitleEdit() {
        stored(RecipeStatus.DRAFT);
        var d = service.update(authorActor, 100L, req("Trứng xào cà chua", null, List.of(line(5))));
        assertThat(d.title()).isEqualTo("Trứng xào cà chua");
        assertThat(d.slug()).isEqualTo("ca-chua-xao-trung");
    }

    @Test
    void editingPublishedSendsBackToPending() {
        stored(RecipeStatus.PUBLISHED);
        var d = service.update(authorActor, 100L, req("Cà chua xào trứng", null, List.of(line(5))));
        assertThat(d.status()).isEqualTo(RecipeStatus.PENDING);
        assertThat(d.publishedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void detailExposesMediaIdsForEditor() {
        when(media.requireOwned(55L, 1L)).thenReturn(withId(new com.recipehub.media.Media(1L, "k", "image/png", 1), 55L));
        var r = new RecipeRequest("Trứng chiên", null, null, 2, 5, 10, Difficulty.EASY, 55L, Set.of(), List.of(line(5)),
                List.of(new RecipeStepRequest("Đập trứng", 55L)));
        var d = service.create(authorActor, r);
        assertThat(d.coverMediaId()).isEqualTo(55L);
        assertThat(d.steps().getFirst().mediaId()).isEqualTo(55L);
    }

    @Test
    void symbolOnlyTitleStillGetsSlug() {
        var d = service.create(authorActor, req("!!!", null, List.of(line(5))));
        assertThat(d.slug()).isEqualTo("cong-thuc");
    }
}
