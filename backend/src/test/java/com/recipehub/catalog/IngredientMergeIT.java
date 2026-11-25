package com.recipehub.catalog;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.user.MailService;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Không @Transactional: merge commit thật (unique recipe/ingredient là DEFERRABLE). */
@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@Import(TestcontainersConfiguration.class)
class IngredientMergeIT {

    @Autowired IngredientService service;
    @Autowired IngredientRepository ingredients;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MailService mail;

    String x;
    Ingredient src;
    Ingredient tgt;
    Ingredient salt;
    User author;

    @BeforeEach
    void data() {
        x = UUID.randomUUID().toString().substring(0, 8);
        src = ingredients.save(new Ingredient("Ca chua " + x, "ca-chua-src-" + x, IngredientGroup.VEGETABLE, null,
                IngredientStatus.PENDING, null));
        tgt = ingredients.save(new Ingredient("Cà chua " + x, "ca-chua-tgt-" + x, IngredientGroup.VEGETABLE, null,
                IngredientStatus.APPROVED, null));
        salt = ingredients.save(new Ingredient("Muối " + x, "muoi-" + x, IngredientGroup.SPICE, null,
                IngredientStatus.APPROVED, null));
        author = users.save(new User("mg" + x + "@x.com", "mg" + x, "{noop}x", "Cook"));
    }

    Long recipe(String slug, Object... ingredientAndQty) {
        Recipe r = new Recipe(author, slug, slug + "-" + x);
        for (int i = 0; i < ingredientAndQty.length; i += 2) {
            r.addIngredient((Ingredient) ingredientAndQty[i], new BigDecimal((String) ingredientAndQty[i + 1]),
                    Unit.G, null);
        }
        return recipes.save(r).getId();
    }

    List<Map<String, Object>> lines(Long recipeId) {
        return jdbc.queryForList("select ingredient_id, quantity from recipe_ingredients where recipe_id = ? order by position",
                recipeId);
    }

    @Test
    void mergeRepointsReferences() {
        Long id = recipe("r1", src, "200", salt, "5");
        service.merge(src.getId(), tgt.getId());
        assertThat(lines(id)).extracting(m -> ((Number) m.get("ingredient_id")).longValue())
                .containsExactly(tgt.getId(), salt.getId());
        assertThat(ingredients.existsById(src.getId())).isFalse();
    }

    @Test
    void mergeDropsDuplicateWhenRecipeHasBoth() {
        Long id = recipe("r2", src, "200", tgt, "300");
        service.merge(src.getId(), tgt.getId());
        var rows = lines(id);
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.getFirst().get("ingredient_id")).longValue()).isEqualTo(tgt.getId());
        assertThat((BigDecimal) rows.getFirst().get("quantity")).isEqualByComparingTo("300");
    }

    @Test
    void mergeIntoSelfRejected() {
        assertThatThrownBy(() -> service.merge(src.getId(), src.getId())).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        assertThatThrownBy(() -> service.merge(src.getId(), -1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(ingredients.existsById(src.getId())).isTrue();
    }
}
