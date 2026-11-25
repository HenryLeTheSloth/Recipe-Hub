package com.recipehub.catalog;

import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient extends BaseEntity {

    private String name;
    private String slug;
    @Enumerated(EnumType.STRING)
    private IngredientGroup ingredientGroup; // cột ingredient_group: "group" là từ khóa SQL/JPQL
    private String emoji;
    @Enumerated(EnumType.STRING)
    private IngredientStatus status;
    private Long createdBy; // users.id, ON DELETE SET NULL

    protected Ingredient() {
    }

    public Ingredient(String name, String slug, IngredientGroup group, String emoji, IngredientStatus status,
                      Long createdBy) {
        this.name = name;
        this.slug = slug;
        this.ingredientGroup = group;
        this.emoji = emoji;
        this.status = status;
        this.createdBy = createdBy;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public IngredientGroup getIngredientGroup() { return ingredientGroup; }
    public String getEmoji() { return emoji; }
    public IngredientStatus getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setIngredientGroup(IngredientGroup group) { this.ingredientGroup = group; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public void setStatus(IngredientStatus status) { this.status = status; }
}
