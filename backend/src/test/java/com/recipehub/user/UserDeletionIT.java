package com.recipehub.user;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.community.Comment;
import com.recipehub.community.CommentRepository;
import com.recipehub.community.Favorite;
import com.recipehub.community.FavoriteId;
import com.recipehub.community.FavoriteRepository;
import com.recipehub.community.Rating;
import com.recipehub.community.RatingRepository;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Không @Transactional: ràng buộc FK (RESTRICT/CASCADE) chỉ được kiểm khi commit thật. */
@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@Import(TestcontainersConfiguration.class)
class UserDeletionIT {

    @Autowired AdminUserService admin;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired CommentRepository comments;
    @Autowired RatingRepository ratings;
    @Autowired FavoriteRepository favorites;
    @MockitoBean MailService mail;

    final AppUserDetails actor = new AppUserDetails(-1L, "root@test.local", "x", true, false, Set.of());

    User newUser() {
        String u = "del" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(new User(u + "@x.com", u, "{noop}x", "Del"));
    }

    Recipe recipeOf(User author) {
        return recipes.save(new Recipe(author, "Món", "mon-" + UUID.randomUUID()));
    }

    void assertHasContent(User u) {
        assertThatThrownBy(() -> admin.delete(actor, u.getId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_HAS_CONTENT));
        assertThat(users.existsById(u.getId())).isTrue();
    }

    @Test
    void userWithCommentCannotBeDeleted() {
        Recipe r = recipeOf(newUser());
        User u = newUser();
        comments.save(new Comment(r, u, null, "hi"));
        assertHasContent(u);
    }

    @Test
    void userWithRatingCannotBeDeleted() {
        Recipe r = recipeOf(newUser());
        User u = newUser();
        ratings.save(new Rating(u, r, 5, null));
        assertHasContent(u);
    }

    @Test
    void userWithOnlyFavoritesIsDeleted() {
        Recipe r = recipeOf(newUser());
        User u = newUser();
        favorites.save(new Favorite(u, r));
        admin.delete(actor, u.getId());
        assertThat(users.existsById(u.getId())).isFalse();
        assertThat(favorites.existsById(new FavoriteId(u.getId(), r.getId()))).isFalse();
        assertThat(favorites.count()).isZero();
    }
}
