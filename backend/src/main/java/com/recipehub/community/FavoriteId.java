package com.recipehub.community;

import jakarta.persistence.Embeddable;

@Embeddable
public record FavoriteId(Long userId, Long recipeId) {
}
