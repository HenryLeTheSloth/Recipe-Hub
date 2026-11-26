package com.recipehub.community;

import com.recipehub.recipe.Recipe;
import com.recipehub.user.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Recipe recipe;

    @CreationTimestamp
    private Instant createdAt;

    protected Favorite() {
    }

    public Favorite(User user, Recipe recipe) {
        this.id = new FavoriteId(user.getId(), recipe.getId());
        this.user = user;
        this.recipe = recipe;
    }
}
