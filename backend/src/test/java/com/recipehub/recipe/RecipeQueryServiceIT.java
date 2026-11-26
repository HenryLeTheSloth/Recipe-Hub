package com.recipehub.recipe;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.catalog.CategoryRepository;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientRepository;
import com.recipehub.catalog.IngredientStatus;
import com.recipehub.catalog.Tag;
import com.recipehub.catalog.TagRepository;
import com.recipehub.catalog.Unit;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeSearch;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.MailService;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mỗi test dựng dữ liệu riêng (nguyên liệu/tag/tiêu đề có hậu tố ngẫu nhiên) nên không phụ thuộc IT khác cùng DB. */
@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@Import(TestcontainersConfiguration.class)
class RecipeQueryServiceIT {

    @Autowired RecipeQueryService service;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TagRepository tags;
    @Autowired IngredientRepository ingredients;
    @Autowired jakarta.persistence.EntityManagerFactory emf;
    @MockitoBean MailService mail;

    String x;
    User author;
    User mod;
    Ingredient egg;
    Ingredient tomato;
    Ingredient onion;
    Tag tag;

    @BeforeEach
    void data() {
        x = UUID.randomUUID().toString().substring(0, 8);
        author = users.save(new User("a" + x + "@x.com", "a" + x, "{noop}x", "Tác giả"));
        mod = users.save(new User("m" + x + "@x.com", "m" + x, "{noop}x", "Mod"));
        egg = ing("Trứng " + x);
        tomato = ing("Cà chua " + x);
        onion = ing("Hành " + x);
        tag = tags.save(new Tag("Nhanh " + x, "nhanh-" + x));
        save("A " + x, "a-" + x, Instant.parse("2026-11-01T00:00:00Z"), egg, tomato);
        save("B " + x, "b-" + x, Instant.parse("2026-11-03T00:00:00Z"), egg);
        save("Trứng chiên " + x, "trung-chien-" + x, Instant.parse("2026-11-02T00:00:00Z"), egg, tomato, onion);
        save("Nháp " + x, "nhap-" + x, null, egg, tomato);
    }

    Ingredient ing(String name) {
        return ingredients.save(new Ingredient(name, com.recipehub.common.Slugs.slugify(name), IngredientGroup.OTHER, null,
                IngredientStatus.APPROVED, null));
    }

    /** publishedAt null → để DRAFT. */
    void save(String title, String slug, Instant publishedAt, Ingredient... lines) {
        Recipe r = new Recipe(author, title, slug);
        r.setCategory(categories.findBySlug("mon-chinh").orElseThrow());
        r.getTags().add(tag);
        for (Ingredient i : lines) r.addIngredient(i, BigDecimal.ONE, Unit.PIECE, null);
        r.addStep("Nấu", null);
        if (publishedAt != null) {
            r.submit();
            r.approve(mod, publishedAt);
        }
        recipes.save(r);
    }

    Page<RecipeCard> search(String q, String category, String tagSlug, List<Long> ids) {
        return service.search(new RecipeSearch(q, category, tagSlug, ids, null), PageRequest.of(0, 12));
    }

    AppUserDetails viewer(User u, String... perms) {
        return new AppUserDetails(u.getId(), u.getEmail(), "x", true, false, Set.of(perms));
    }

    @Test
    void containsAllIngredients() {
        Page<RecipeCard> p = search(null, null, null, List.of(egg.getId(), tomato.getId()));
        assertThat(p.getContent()).extracting(RecipeCard::slug).containsExactlyInAnyOrder("a-" + x, "trung-chien-" + x);
        assertThat(p.getTotalElements()).isEqualTo(2);
        assertThat(search(null, null, null, List.of(egg.getId(), egg.getId(), tomato.getId())).getTotalElements())
                .as("id lặp không làm hỏng phép đếm").isEqualTo(2);
    }

    @Test
    void qMatchesWithoutDiacritics() {
        assertThat(search("trung chien " + x, null, null, null).getContent()).extracting(RecipeCard::slug)
                .containsExactly("trung-chien-" + x);
        assertThat(search("Trứng chiên " + x, null, null, null).getTotalElements()).isEqualTo(1);
    }

    @Test
    void accentedQueryMatchesTitleOnly() {
        save("Canh " + x + " gà", "canh-" + x + "-ga", Instant.parse("2026-11-04T00:00:00Z"), egg);
        save("Canh " + x + " gạo", "canh-" + x + "-gao", Instant.parse("2026-11-04T00:00:00Z"), egg);
        assertThat(search("canh " + x + " gà", null, null, null).getContent()).extracting(RecipeCard::slug)
                .containsExactly("canh-" + x + "-ga");
        String decomposed = java.text.Normalizer.normalize("canh " + x + " gà", java.text.Normalizer.Form.NFD);
        assertThat(search(decomposed, null, null, null).getContent()).as("gõ Unicode tổ hợp").extracting(RecipeCard::slug)
                .containsExactly("canh-" + x + "-ga");
        assertThat(search("canh\u00a0" + x + "\u00a0ga", null, null, null).getTotalElements()).as("NBSP, không dấu").isEqualTo(2);
    }

    @Test
    void asciiQueryMatchesCurrentTitleNotSlug() {
        Instant t = Instant.parse("2026-11-05T00:00:00Z");
        save("Canh cá " + x, "canh-ga-" + x, t, egg); // đổi tên sau khi đăng, slug giữ tên cũ
        save("Canh cá " + x, "canh-ca-" + x + "-2", t, egg); // slug trùng → hậu tố -2
        assertThat(search("canh ga " + x, null, null, null).getTotalElements()).as("tên cũ").isZero();
        assertThat(search("canh ca " + x + " 2", null, null, null).getTotalElements()).as("hậu tố slug").isZero();
        assertThat(search("canh ca " + x, null, null, null).getTotalElements()).isEqualTo(2);
    }

    @Test
    void detailQueryCountDoesNotGrowWithIngredients() {
        save("Một " + x, "mot-" + x, Instant.parse("2026-11-05T00:00:00Z"), egg);
        var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        service.getBySlug("mot-" + x, null);
        long one = stats.getPrepareStatementCount();
        stats.clear();
        service.getBySlug("trung-chien-" + x, null); // 3 nguyên liệu
        assertThat(stats.getPrepareStatementCount()).isEqualTo(one);
    }

    @Test
    void filtersByCategoryAndTag() {
        assertThat(search(null, "mon-chinh", "nhanh-" + x, null).getTotalElements()).isEqualTo(3);
        assertThat(search(null, "mon-canh", "nhanh-" + x, null).getTotalElements()).isZero();
    }

    @Test
    void draftsNeverListed() {
        assertThat(search("nhap " + x, null, null, null).getTotalElements()).isZero();
        assertThat(search(null, null, "nhanh-" + x, null).getContent()).extracting(RecipeCard::status)
                .containsOnly(RecipeStatus.PUBLISHED);
    }

    @Test
    void draftSlugIs404ForAnonymousAndOthers() {
        User other = users.save(new User("o" + x + "@x.com", "o" + x, "{noop}x", "Khác"));
        for (AppUserDetails v : new AppUserDetails[]{null, viewer(other, "RECIPE_WRITE")}) {
            assertThatThrownBy(() -> service.getBySlug("nhap-" + x, v)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
        }
        assertThat(service.getBySlug("a-" + x, null).title()).isEqualTo("A " + x);
    }

    @Test
    void draftVisibleToAuthorAndModerator() {
        assertThat(service.getBySlug("nhap-" + x, viewer(author)).status()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(service.getBySlug("nhap-" + x, viewer(mod, "CONTENT_MODERATE")).status()).isEqualTo(RecipeStatus.DRAFT);
    }

    @Test
    void newestFirstByPublishedAt() {
        assertThat(search(null, null, null, List.of(egg.getId())).getContent()).extracting(RecipeCard::slug)
                .containsExactly("b-" + x, "trung-chien-" + x, "a-" + x);
    }
}
