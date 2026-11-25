package com.recipehub.recipe;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.catalog.CategoryRepository;
import com.recipehub.catalog.IngredientRepository;
import com.recipehub.catalog.Unit;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.recipe.dto.RecipeIngredientRequest;
import com.recipehub.recipe.dto.RecipeRequest;
import com.recipehub.recipe.dto.RecipeStepRequest;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.MailService;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Không @Transactional: mỗi lời gọi service commit thật, để ràng buộc unique DEFERRABLE được kiểm ở commit. */
@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@Import(TestcontainersConfiguration.class)
class RecipeServiceIT {

    @Autowired RecipeService service;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired IngredientRepository ingredients;
    @Autowired TransactionTemplate tx;
    @MockitoBean MailService mail;

    AppUserDetails newAuthor() {
        String u = "it" + UUID.randomUUID().toString().substring(0, 8);
        User saved = users.save(new User(u + "@x.com", u, "{noop}x", "Cook"));
        return new AppUserDetails(saved.getId(), saved.getEmail(), "x", true, false, Set.of("RECIPE_WRITE"));
    }

    long ing(String slug) {
        return ingredients.findBySlug(slug).orElseThrow().getId();
    }

    RecipeRequest req(List<String> ingredientSlugs, List<String> steps) {
        return new RecipeRequest("Cà chua xào trứng", "Món nhanh", categories.findBySlug("mon-chinh").orElseThrow().getId(),
                2, 5, 10, Difficulty.EASY, null, Set.of(),
                ingredientSlugs.stream().map(s -> new RecipeIngredientRequest(ing(s), new BigDecimal("2"), Unit.PIECE, null)).toList(),
                steps.stream().map(c -> new RecipeStepRequest(c, null)).toList());
    }

    @Test
    void replacingStepsAndIngredientsCommits() {
        AppUserDetails a = newAuthor();
        RecipeDetail d = service.create(a, req(List.of("trung-ga", "ca-chua"), List.of("A", "B", "C")));
        service.update(a, d.id(), req(List.of("ca-chua", "hanh-la"), List.of("X", "Y")));

        RecipeDetail back = service.getMine(a, d.id());
        assertThat(back.steps()).extracting(RecipeDetail.StepView::stepNo, RecipeDetail.StepView::content)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "X"), org.assertj.core.groups.Tuple.tuple(2, "Y"));
        assertThat(back.ingredients()).extracting(RecipeDetail.IngredientLine::name).containsExactly("Cà chua", "Hành lá");
    }

    @Test
    void sameTitleGetsNumberedSlug() {
        AppUserDetails a = newAuthor();
        String first = service.create(a, req(List.of("trung-ga"), List.of("A"))).slug();
        String second = service.create(a, req(List.of("trung-ga"), List.of("A"))).slug();
        assertThat(first).startsWith("ca-chua-xao-trung");
        assertThat(second).isNotEqualTo(first).startsWith("ca-chua-xao-trung-");
    }

    @Test
    void editingPublishedRecipeReturnsToPending() {
        AppUserDetails a = newAuthor();
        RecipeDetail d = service.create(a, req(List.of("trung-ga"), List.of("A")));
        service.submit(a, d.id());
        Instant approvedAt = Instant.parse("2026-11-01T00:00:00Z");
        tx.executeWithoutResult(s -> recipes.findById(d.id()).orElseThrow()
                .approve(users.getReferenceById(a.id()), approvedAt));

        RecipeDetail edited = service.update(a, d.id(), req(List.of("trung-ga", "ca-chua"), List.of("A", "B")));
        assertThat(edited.status()).isEqualTo(RecipeStatus.PENDING);
        assertThat(service.getMine(a, d.id()).publishedAt()).isEqualTo(approvedAt);
    }
}
