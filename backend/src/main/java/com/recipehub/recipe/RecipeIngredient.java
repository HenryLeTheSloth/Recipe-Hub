package com.recipehub.recipe;

import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.Unit;
import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Ingredient ingredient;

    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    private Unit unit;

    private String note;
    private int position;

    protected RecipeIngredient() {
    }

    RecipeIngredient(Recipe recipe, Ingredient ingredient, BigDecimal quantity, Unit unit, String note, int position) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.note = note;
        this.position = position;
    }

    public Recipe getRecipe() { return recipe; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getQuantity() { return quantity; }
    public Unit getUnit() { return unit; }
    public String getNote() { return note; }
    public int getPosition() { return position; }
}
