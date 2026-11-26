package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.community.dto.RatingRequest;
import com.recipehub.community.dto.RatingView;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeMapper;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeService;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.UserRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RatingService {

    private final RatingRepository ratings;
    private final RecipeRepository recipes;
    private final UserRepository users;
    private final RecipeMapper mapper;

    public RatingService(RatingRepository ratings, RecipeRepository recipes, UserRepository users, RecipeMapper mapper) {
        this.ratings = ratings;
        this.recipes = recipes;
        this.users = users;
        this.mapper = mapper;
    }

    /** Lần đầu tạo, lần sau cập nhật đánh giá cũ (giữ nguyên trạng thái ẩn). */
    public RatingView upsert(AppUserDetails actor, Long recipeId, RatingRequest req) {
        Recipe recipe = published(recipeId);
        if (recipe.getAuthor().getId().equals(actor.id())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không thể tự đánh giá công thức của mình");
        }
        Rating rating = ratings.findByUser_IdAndRecipe_Id(actor.id(), recipeId).orElse(null);
        if (rating == null) {
            rating = new Rating(users.getReferenceById(actor.id()), recipe, req.stars(), req.review());
        } else {
            rating.update(req.stars(), req.review());
        }
        return view(ratings.save(rating));
    }

    /** Đánh giá của chính người xem (kể cả đã bị ẩn), rỗng nếu chưa chấm. */
    @Transactional(readOnly = true)
    public java.util.Optional<RatingView> mine(AppUserDetails actor, Long recipeId) {
        published(recipeId);
        return ratings.findByUser_IdAndRecipe_Id(actor.id(), recipeId).map(this::view);
    }

    public void deleteMine(AppUserDetails actor, Long recipeId) {
        ratings.findByUser_IdAndRecipe_Id(actor.id(), recipeId).ifPresent(ratings::delete);
    }

    /** Đánh giá bị ẩn chỉ moderator thấy. */
    @Transactional(readOnly = true)
    public Page<RatingView> list(Long recipeId, @Nullable AppUserDetails viewer, Pageable p) {
        published(recipeId);
        Pageable page = PageRequest.of(p.getPageNumber(), p.getPageSize(),
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        boolean moderator = viewer != null && viewer.can(RecipeService.MODERATE);
        return (moderator ? ratings.findByRecipe_Id(recipeId, page) : ratings.findByRecipe_IdAndHiddenFalse(recipeId, page))
                .map(this::view);
    }

    public RatingView setHidden(Long ratingId, boolean hidden) {
        Rating rating = ratings.findById(ratingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy đánh giá"));
        rating.setHidden(hidden);
        return view(rating);
    }

    private Recipe published(Long recipeId) {
        return recipes.findById(recipeId).filter(r -> r.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
    }

    private RatingView view(Rating r) {
        return new RatingView(r.getId(), r.getStars(), r.getReview(), mapper.author(r.getUser()), r.isHidden(),
                r.getUpdatedAt());
    }
}
