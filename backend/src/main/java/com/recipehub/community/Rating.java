package com.recipehub.community;

import com.recipehub.common.BaseEntity;
import com.recipehub.recipe.Recipe;
import com.recipehub.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ratings")
public class Rating extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Recipe recipe;

    private int stars;
    private String review;
    private boolean hidden;

    protected Rating() {
    }

    public Rating(User user, Recipe recipe, int stars, String review) {
        this.user = user;
        this.recipe = recipe;
        this.stars = stars;
        this.review = review;
    }

    public void update(int stars, String review) {
        this.stars = stars;
        this.review = review;
        touch();
    }

    public User getUser() { return user; }
    public Recipe getRecipe() { return recipe; }
    public int getStars() { return stars; }
    public String getReview() { return review; }
    public boolean isHidden() { return hidden; }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        touch();
    }
}
