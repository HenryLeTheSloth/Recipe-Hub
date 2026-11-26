package com.recipehub.community;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.util.List;
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
class RatingRepositoryTest {

    @Autowired RatingRepository ratings;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;

    User user(String n) {
        return users.save(new User(n + "@x.com", n, "{noop}x", n));
    }

    Recipe recipe() {
        return recipes.save(new Recipe(user("author"), "Món", "mon"));
    }

    @Test
    void statsIgnoreHidden() {
        Recipe r = recipe();
        ratings.save(new Rating(user("u1"), r, 5, null));
        ratings.save(new Rating(user("u2"), r, 3, null));
        Rating h = new Rating(user("u3"), r, 1, null);
        h.setHidden(true);
        ratings.saveAndFlush(h);
        assertThat(ratings.statsFor(List.of(r.getId()))).containsExactly(new RatingStats(r.getId(), 4.0, 2));
    }

    @Test
    void statsAverageRoundedToOneDecimal() {
        assertThat(new RatingStats(1L, 4.3333, 3).avg()).isEqualTo(4.3);
    }

    @Test
    void oneRatingPerUserPerRecipe() {
        Recipe r = recipe();
        User u = user("u1");
        ratings.saveAndFlush(new Rating(u, r, 5, null));
        assertThatThrownBy(() -> ratings.saveAndFlush(new Rating(u, r, 4, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void starsOutOfRangeRejectedByDb() {
        Recipe r = recipe();
        User u = user("u1");
        assertThatThrownBy(() -> ratings.saveAndFlush(new Rating(u, r, 6, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
