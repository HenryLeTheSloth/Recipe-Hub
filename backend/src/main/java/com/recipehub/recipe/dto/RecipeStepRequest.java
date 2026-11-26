package com.recipehub.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Một bước; stepNo = chỉ số + 1. */
public record RecipeStepRequest(@NotBlank @Size(max = 2000) String content, Long mediaId) {
}
