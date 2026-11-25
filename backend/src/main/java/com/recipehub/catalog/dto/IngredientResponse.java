package com.recipehub.catalog.dto;

import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientStatus;

public record IngredientResponse(Long id, String name, String slug, IngredientGroup group, String emoji,
                                 IngredientStatus status) {
}
