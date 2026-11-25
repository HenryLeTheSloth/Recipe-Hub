package com.recipehub.catalog.dto;

import com.recipehub.catalog.IngredientGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredientRequest(@NotBlank @Size(max = 60) String name, @NotNull IngredientGroup group,
                                @Size(max = 16) String emoji) {
}
