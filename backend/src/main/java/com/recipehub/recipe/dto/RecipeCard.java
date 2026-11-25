package com.recipehub.recipe.dto;

import com.recipehub.recipe.Difficulty;
import com.recipehub.recipe.RecipeStatus;
import java.time.Instant;

public record RecipeCard(Long id, String slug, String title, String summary, String coverUrl, String categoryName,
                         Difficulty difficulty, int totalMinutes, String authorName, RecipeStatus status,
                         Instant publishedAt) {
}
