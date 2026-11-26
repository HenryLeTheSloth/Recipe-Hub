package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeQueryService;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final RecipeRepository recipes;
    private final UserRepository users;
    private final RecipeQueryService query;

    public FavoriteService(FavoriteRepository favorites, RecipeRepository recipes, UserRepository users,
                           RecipeQueryService query) {
        this.favorites = favorites;
        this.recipes = recipes;
        this.users = users;
        this.query = query;
    }

    public void add(AppUserDetails actor, Long recipeId) {
        Recipe recipe = recipes.findById(recipeId).filter(r -> r.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
        if (!isFavorite(actor.id(), recipeId)) {
            favorites.save(new Favorite(users.getReferenceById(actor.id()), recipe));
        }
    }

    public void remove(AppUserDetails actor, Long recipeId) {
        favorites.deleteById(new FavoriteId(actor.id(), recipeId));
    }

    @Transactional(readOnly = true)
    public Page<RecipeCard> listMine(AppUserDetails actor, Pageable p) {
        return query.cards(favorites.publishedBy(actor.id(), PageRequest.of(p.getPageNumber(), p.getPageSize())));
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long recipeId) {
        return favorites.existsById(new FavoriteId(userId, recipeId));
    }
}
