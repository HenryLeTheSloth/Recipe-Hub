package com.recipehub.community.dto;

import com.recipehub.recipe.dto.RecipeDetail.AuthorView;
import java.time.Instant;
import java.util.List;

/** {@code replies} luôn rỗng ở cấp trả lời. */
public record CommentView(Long id, String content, AuthorView author, boolean hidden, Instant createdAt,
                          List<CommentView> replies) {
}
