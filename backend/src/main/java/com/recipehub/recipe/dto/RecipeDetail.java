package com.recipehub.recipe.dto;

import com.recipehub.catalog.Unit;
import com.recipehub.catalog.dto.CategoryResponse;
import com.recipehub.catalog.dto.TagResponse;
import com.recipehub.recipe.Difficulty;
import com.recipehub.recipe.RecipeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecipeDetail(Long id, String slug, String title, String summary, CategoryResponse category,
                           List<TagResponse> tags, int servings, int prepMinutes, int cookMinutes,
                           Difficulty difficulty, RecipeStatus status, String coverUrl, AuthorView author,
                           List<IngredientLine> ingredients, List<StepView> steps, String rejectionReason,
                           Instant publishedAt, Instant createdAt, Instant updatedAt) {

    public record AuthorView(Long id, String username, String displayName, String avatarUrl) {
    }

    public record IngredientLine(Long ingredientId, String name, String emoji, BigDecimal quantity, Unit unit,
                                 String note) {
    }

    public record StepView(int stepNo, String content, String imageUrl) {
    }
}
