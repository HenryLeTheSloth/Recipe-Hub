package com.recipehub.recipe.dto;

import com.recipehub.recipe.Difficulty;
import com.recipehub.recipe.RecipeStatus;
import java.time.Instant;

public record RecipeCard(Long id, String slug, String title, String summary, String coverUrl, String categoryName,
                         Difficulty difficulty, int totalMinutes, String authorName, RecipeStatus status,
                         Instant publishedAt, Double avgRating, long ratingCount) {

    public RecipeCard withRating(Double avgRating, long ratingCount) {
        return new RecipeCard(id, slug, title, summary, coverUrl, categoryName, difficulty, totalMinutes, authorName,
                status, publishedAt, avgRating, ratingCount);
    }
}
