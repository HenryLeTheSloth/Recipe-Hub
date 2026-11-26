package com.recipehub.community.dto;

import com.recipehub.recipe.dto.RecipeDetail.AuthorView;
import java.time.Instant;

public record RatingView(Long id, int stars, String review, AuthorView author, boolean hidden, Instant updatedAt) {
}
