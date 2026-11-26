package com.recipehub.recipe;

import com.recipehub.common.BaseEntity;
import com.recipehub.media.Media;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_steps")
public class RecipeStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Recipe recipe;

    private int stepNo;
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private Media media;

    protected RecipeStep() {
    }

    RecipeStep(Recipe recipe, int stepNo, String content, Media media) {
        this.recipe = recipe;
        this.stepNo = stepNo;
        this.content = content;
        this.media = media;
    }

    public Recipe getRecipe() { return recipe; }
    public int getStepNo() { return stepNo; }
    public String getContent() { return content; }
    public Media getMedia() { return media; }
}
