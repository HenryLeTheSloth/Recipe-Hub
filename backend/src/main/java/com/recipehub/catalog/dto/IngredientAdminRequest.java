package com.recipehub.catalog.dto;

import com.recipehub.catalog.IngredientGroup;
import com.recipehub.catalog.IngredientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientAdminRequest(@NotBlank @Size(max = 60) String name, @NotNull IngredientGroup group,
                                     @Size(max = 16) String emoji, @NotNull IngredientStatus status) {

    public IngredientRequest ingredient() {
        return new IngredientRequest(name, group, emoji);
    }
}
