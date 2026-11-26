package com.recipehub.community;

import com.recipehub.common.BaseEntity;
import com.recipehub.recipe.Recipe;
import com.recipehub.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    private Comment parent;

    private String content;
    private boolean hidden;

    protected Comment() {
    }

    public Comment(Recipe recipe, User author, Comment parent, String content) {
        this.recipe = recipe;
        this.author = author;
        this.parent = parent;
        this.content = content;
    }

    public Recipe getRecipe() { return recipe; }
    public User getAuthor() { return author; }
    public Comment getParent() { return parent; }
    public String getContent() { return content; }
    public boolean isHidden() { return hidden; }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        touch();
    }
}
