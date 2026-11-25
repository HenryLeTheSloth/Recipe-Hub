package com.recipehub.catalog;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.common.Slugs;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;

import static com.recipehub.catalog.IngredientStatus.APPROVED;
import static com.recipehub.catalog.IngredientStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class IngredientRepositoryTest {

    @Autowired IngredientRepository repo;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;

    List<Ingredient> search(String q, IngredientGroup g) {
        return repo.search(q, Slugs.slugify(q), g, APPROVED, Limit.of(200));
    }

    @Test
    void seedContainsRequiredIngredientsAndCatalog() {
        for (String slug : List.of("trung-ga", "ca-chua", "hanh-la", "nuoc-mam", "thit-ba-chi")) {
            assertThat(repo.findBySlug(slug)).get().extracting(Ingredient::getStatus).isEqualTo(APPROVED);
        }
        assertThat(repo.findBySlug("ca-chua").orElseThrow().getEmoji()).isEqualTo("🍅");
        assertThat(repo.count()).isGreaterThanOrEqualTo(55);
        assertThat(repo.findAll()).allMatch(i -> i.getEmoji() != null)
                .extracting(Ingredient::getIngredientGroup).contains(IngredientGroup.values());
        assertThat(categories.count()).isEqualTo(7);
        assertThat(tags.count()).isEqualTo(6);
        assertThat(Arrays.stream(new String[]{"mon-chinh", "mon-nuong"})).allMatch(s -> categories.findBySlug(s).isPresent());
    }

    @Test
    void nameUniqueIgnoringCase() {
        assertThatThrownBy(() -> repo.saveAndFlush(
                new Ingredient("CÀ CHUA", "ca-chua-x", IngredientGroup.VEGETABLE, null, PENDING, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchWithoutDiacriticsFindsVietnamese() {
        assertThat(search("ca chua", null)).extracting(Ingredient::getName).contains("Cà chua");
        assertThat(search("CÀ", null)).extracting(Ingredient::getName).contains("Cà chua");
        assertThat(search("", IngredientGroup.SAUCE)).isNotEmpty()
                .allMatch(i -> i.getIngredientGroup() == IngredientGroup.SAUCE);
    }

    @Test
    void searchExcludesPending() {
        repo.saveAndFlush(new Ingredient("Lá mơ lông", "la-mo-long", IngredientGroup.VEGETABLE, "🌿", PENDING, null));
        assertThat(search("la mo", null)).isEmpty();
        assertThat(repo.adminSearch(PENDING, "", "", PageRequest.of(0, 10))).extracting(Ingredient::getName)
                .containsExactly("Lá mơ lông");
        assertThat(repo.adminSearch(null, "la mo", "la-mo", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }
}
