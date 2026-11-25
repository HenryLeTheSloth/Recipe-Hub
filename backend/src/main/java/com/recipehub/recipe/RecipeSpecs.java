package com.recipehub.recipe;

import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Điều kiện tìm kiếm công khai, ghép bằng and(). */
final class RecipeSpecs {

    private RecipeSpecs() {
    }

    static Specification<Recipe> published() {
        return (r, q, cb) -> cb.equal(r.get("status"), RecipeStatus.PUBLISHED);
    }

    /** q có dấu khớp tiêu đề; qSlug (không dấu) khớp slug. Ký tự % _ \ trong q được escape. */
    static Specification<Recipe> titleMatches(String q, String qSlug) {
        return (r, query, cb) -> {
            var title = cb.like(cb.lower(r.get("title")), "%" + escape(q.toLowerCase()) + "%", '\\');
            return qSlug.isEmpty() ? title : cb.or(title, cb.like(r.get("slug"), "%" + qSlug + "%"));
        };
    }

    static Specification<Recipe> inCategory(String slug) {
        return (r, q, cb) -> cb.equal(r.join("category").get("slug"), slug);
    }

    static Specification<Recipe> hasTag(String slug) {
        return (r, q, cb) -> cb.equal(r.join("tags").get("slug"), slug); // tag slug duy nhất → không nhân dòng
    }

    /** Công thức có đủ mọi nguyên liệu trong ids (ids đã bỏ trùng). */
    static Specification<Recipe> containsAllIngredients(List<Long> ids) {
        return (r, query, cb) -> {
            var sub = query.subquery(Long.class);
            var ri = sub.from(RecipeIngredient.class);
            var recipeId = ri.get("recipe").<Long>get("id");
            sub.select(recipeId)
                    .where(ri.get("ingredient").get("id").in(ids))
                    .groupBy(recipeId)
                    .having(cb.equal(cb.countDistinct(ri.get("ingredient").get("id")), (long) ids.size()));
            return r.get("id").in(sub);
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
