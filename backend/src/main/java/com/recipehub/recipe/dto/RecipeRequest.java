package com.recipehub.recipe.dto;

import com.recipehub.recipe.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/** Toàn bộ nội dung công thức; lưu là thay thế hết tag/nguyên liệu/bước. */
public record RecipeRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @Size(max = 500) String summary,
        Long categoryId,
        @Min(1) @Max(50) int servings,
        @Min(0) @Max(1440) int prepMinutes,
        @Min(0) @Max(1440) int cookMinutes,
        @NotNull Difficulty difficulty,
        Long coverMediaId,
        @NotNull Set<@NotNull Long> tagIds,
        @NotNull @Size(max = 50) List<@NotNull @Valid RecipeIngredientRequest> ingredients,
        @NotNull @Size(max = 30) List<@NotNull @Valid RecipeStepRequest> steps) {
}
