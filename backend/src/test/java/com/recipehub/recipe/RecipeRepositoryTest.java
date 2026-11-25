package com.recipehub.recipe;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.catalog.CategoryRepository;
import com.recipehub.catalog.IngredientRepository;
import com.recipehub.catalog.TagRepository;
import com.recipehub.catalog.Unit;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class RecipeRepositoryTest {

    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;
    @Autowired IngredientRepository ingredients;
    @Autowired EntityManager em;

    /** Công thức đầy đủ (danh mục, tag, 2 nguyên liệu, 2 bước) đã ghi xuống DB; persistence context đã clear. */
    Recipe saved() {
        User author = users.save(new User("cook@x.com", "cook", "{noop}x", "Cook"));
        Recipe r = new Recipe(author, "Cà chua xào trứng", "ca-chua-xao-trung");
        r.setCategory(categories.findBySlug("mon-chinh").orElseThrow());
        r.getTags().add(tags.findAll().getFirst());
        r.addIngredient(ingredients.findBySlug("trung-ga").orElseThrow(), new BigDecimal("2"), Unit.PIECE, null);
        r.addIngredient(ingredients.findBySlug("ca-chua").orElseThrow(), new BigDecimal("200"), Unit.G, "chín");
        r.addStep("Đập trứng", null);
        r.addStep("Xào", null);
        recipes.saveAndFlush(r);
        em.clear();
        return r;
    }

    long count(String table) {
        return ((Number) em.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    @Test
    void savesChildrenInOrder() {
        Recipe r = recipes.findBySlug(saved().getSlug()).orElseThrow();
        assertThat(r.getIngredients()).extracting(i -> i.getIngredient().getSlug()).containsExactly("trung-ga", "ca-chua");
        assertThat(r.getSteps()).extracting(RecipeStep::getContent).containsExactly("Đập trứng", "Xào");
        assertThat(r.getIngredients().get(1).getQuantity()).isEqualByComparingTo("200");
        assertThat(r.getStatus()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(recipes.existsBySlug("ca-chua-xao-trung")).isTrue();
    }

    @Test
    void deletingRecipeCascadesChildren() {
        Long id = saved().getId();
        assertThat(count("recipe_ingredients")).isEqualTo(2);
        recipes.deleteById(id);
        recipes.flush();
        assertThat(count("recipe_ingredients")).isZero();
        assertThat(count("recipe_steps")).isZero();
        assertThat(count("recipe_tags")).isZero();
    }

    @Test
    void categoryInUseCannotBeDeleted() {
        saved();
        assertThatThrownBy(() -> {
            categories.delete(categories.findBySlug("mon-chinh").orElseThrow());
            categories.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void authorWithRecipeCannotBeDeleted() {
        saved();
        assertThatThrownBy(() -> {
            users.delete(users.findByEmail("cook@x.com").orElseThrow());
            users.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ingredientInUseCannotBeDeleted() {
        saved();
        assertThatThrownBy(() -> {
            ingredients.delete(ingredients.findBySlug("trung-ga").orElseThrow());
            ingredients.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void servingsOutOfRangeRejectedByDb() {
        User author = users.save(new User("c2@x.com", "cook2", "{noop}x", "C"));
        Recipe r = new Recipe(author, "Món", "mon");
        r.setServings(0);
        assertThatThrownBy(() -> recipes.saveAndFlush(r)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
