package com.recipehub.recipe;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.common.Slugs;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.recipe.dto.RecipeSearch;
import com.recipehub.user.AppUserDetails;
import java.text.Normalizer;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc công khai: danh sách chỉ PUBLISHED; chi tiết bản chưa đăng chỉ tác giả/moderator thấy. */
@Service
@Transactional(readOnly = true)
public class RecipeQueryService {

    private final RecipeRepository recipes;
    private final RecipeMapper mapper;

    public RecipeQueryService(RecipeRepository recipes, RecipeMapper mapper) {
        this.recipes = recipes;
        this.mapper = mapper;
    }

    public Page<RecipeCard> search(RecipeSearch s, Pageable p) {
        Specification<Recipe> spec = RecipeSpecs.published();
        if (s.q() != null && !s.q().isBlank()) {
            String q = Normalizer.normalize(s.q(), Normalizer.Form.NFC).strip().replaceAll("(?U)\\s+", " ");
            spec = spec.and(RecipeSpecs.titleMatches(q, Slugs.hasDiacritics(q) ? "" : Slugs.slugify(q))); // có dấu → chỉ khớp tiêu đề
        }
        if (s.category() != null && !s.category().isBlank()) spec = spec.and(RecipeSpecs.inCategory(s.category()));
        if (s.tag() != null && !s.tag().isBlank()) spec = spec.and(RecipeSpecs.hasTag(s.tag()));
        if (s.ingredientIds() != null && !s.ingredientIds().isEmpty()) {
            List<Long> ids = s.ingredientIds().stream().distinct().toList();
            spec = spec.and(RecipeSpecs.containsAllIngredients(ids));
        }
        // ponytail: chỉ "newest"; GĐ4 thêm "rating" ở đây.
        Sort sort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        return recipes.findAll(spec, PageRequest.of(p.getPageNumber(), p.getPageSize(), sort)).map(mapper::card);
    }

    public RecipeDetail getBySlug(String slug, @Nullable AppUserDetails viewer) {
        return recipes.findBySlug(slug)
                .filter(r -> r.getStatus() == RecipeStatus.PUBLISHED || viewer != null
                        && (r.getAuthor().getId().equals(viewer.id()) || viewer.can(RecipeService.MODERATE)))
                .map(mapper::detail)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
    }
}
