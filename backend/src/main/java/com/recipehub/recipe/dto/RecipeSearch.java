package com.recipehub.recipe.dto;

import java.util.List;

/** Tham số tìm kiếm công khai; sort: "newest" (mặc định). */
public record RecipeSearch(String q, String category, String tag, List<Long> ingredientIds, String sort) {
}
