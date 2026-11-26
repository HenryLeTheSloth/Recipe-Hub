package com.recipehub.community;

import com.recipehub.TestcontainersConfiguration;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.MailService;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.admin.email=root@test.local", "app.admin.password=RootPass123"})
@Import(TestcontainersConfiguration.class)
class FavoriteIT {

    @Autowired FavoriteService service;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired CommentRepository comments;
    @MockitoBean MailService mail;

    User newUser() {
        String u = "fav" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(new User(u + "@x.com", u, "{noop}x", "Fav"));
    }

    Recipe recipe(User author, RecipeStatus status) {
        Recipe r = new Recipe(author, "Món", "mon-" + UUID.randomUUID());
        ReflectionTestUtils.setField(r, "status", status);
        return recipes.save(r);
    }

    @Test
    void listMineIsPublishedOnlyNewestFavoritedFirst() throws Exception {
        User cook = newUser();
        User me = newUser();
        var actor = new AppUserDetails(me.getId(), me.getEmail(), "x", true, false, Set.of());
        Recipe a = recipe(cook, RecipeStatus.PUBLISHED);
        Recipe b = recipe(cook, RecipeStatus.PUBLISHED);
        Recipe pending = recipe(cook, RecipeStatus.PENDING);
        service.add(actor, a.getId());
        Thread.sleep(20);
        service.add(actor, b.getId());
        // yêu thích rồi bị gỡ khỏi công khai: không hiện nữa
        Recipe c = recipe(cook, RecipeStatus.PUBLISHED);
        service.add(actor, c.getId());
        ReflectionTestUtils.setField(c, "status", RecipeStatus.HIDDEN);
        recipes.save(c);

        var page = service.listMine(actor, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(RecipeCard::id).containsExactly(b.getId(), a.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(service.isFavorite(me.getId(), pending.getId())).isFalse();
    }

    @Test
    void deletingRootCommentCascadesReplies() {
        User cook = newUser();
        Recipe r = recipe(cook, RecipeStatus.PUBLISHED);
        Comment root = comments.save(new Comment(r, cook, null, "gốc"));
        Comment reply = comments.save(new Comment(r, cook, root, "trả lời"));
        comments.delete(root);
        comments.flush();
        assertThat(comments.existsById(reply.getId())).isFalse();
    }
}
