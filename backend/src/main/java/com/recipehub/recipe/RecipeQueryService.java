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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.recipehub.community.FavoriteId;
import com.recipehub.community.FavoriteRepository;
import com.recipehub.community.RatingRepository;
import com.recipehub.community.RatingStats;
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
    private final RatingRepository ratings;
    private final FavoriteRepository favorites;

    public RecipeQueryService(RecipeRepository recipes, RecipeMapper mapper, RatingRepository ratings,
                             FavoriteRepository favorites) {
        this.favorites = favorites;
        this.recipes = recipes;
        this.ratings = ratings;
        this.mapper = mapper;
    }

    public Page<RecipeCard> search(RecipeSearch s, Pageable p) {
        Specification<Recipe> spec = RecipeSpecs.published();
        if (s.q() != null && !s.q().isBlank()) {
            String q = Normalizer.normalize(s.q(), Normalizer.Form.NFC).strip().replaceAll("(?U)\\s+", " ");
            spec = spec.and(RecipeSpecs.titleMatches(q, Slugs.hasDiacritics(q)));
        }
        if (s.category() != null && !s.category().isBlank()) spec = spec.and(RecipeSpecs.inCategory(s.category()));
        if (s.tag() != null && !s.tag().isBlank()) spec = spec.and(RecipeSpecs.hasTag(s.tag()));
        if (s.ingredientIds() != null && !s.ingredientIds().isEmpty()) {
            List<Long> ids = s.ingredientIds().stream().distinct().toList();
            spec = spec.and(RecipeSpecs.containsAllIngredients(ids));
        }
        Sort newest = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        if ("rating".equals(s.sort())) {
            spec = spec.and(RecipeSpecs.orderByRating()); // thứ tự nằm trong spec; hòa điểm → mới nhất trước
            newest = Sort.unsorted();
        }
        return cards(recipes.findAll(spec, PageRequest.of(p.getPageNumber(), p.getPageSize(), newest)));
    }

    /** Trang công thức → thẻ kèm điểm trung bình, lấy điểm một lần cho cả trang (dùng chung cho danh sách yêu thích). */
    public Page<RecipeCard> cards(Page<Recipe> page) {
        Map<Long, RatingStats> stats = ratings.statsFor(page.map(Recipe::getId).getContent()).stream()
                .collect(Collectors.toMap(RatingStats::recipeId, Function.identity()));
        return page.map(r -> {
            RatingStats st = stats.get(r.getId());
            return mapper.card(r).withRating(st == null ? null : st.avg(), st == null ? 0 : st.count());
        });
    }

    public RecipeDetail getBySlug(String slug, @Nullable AppUserDetails viewer) {
        return recipes.findBySlug(slug)
                .filter(r -> r.getStatus() == RecipeStatus.PUBLISHED || viewer != null
                        && (r.getAuthor().getId().equals(viewer.id()) || viewer.can(RecipeService.MODERATE)))
                .map(r -> {
                    var st = ratings.statsFor(List.of(r.getId()));
                    return mapper.detail(r).withRating(st.isEmpty() ? null : st.getFirst().avg(),
                            st.isEmpty() ? 0 : st.getFirst().count())
                            .withFavorited(viewer != null && favorites.existsById(new FavoriteId(viewer.id(), r.getId())));
                })
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
    }
}
