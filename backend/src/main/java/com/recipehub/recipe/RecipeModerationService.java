package com.recipehub.recipe;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.time.Clock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hàng đợi duyệt và các thao tác của moderator; quyền CONTENT_MODERATE kiểm ở controller. */
@Service
@Transactional
public class RecipeModerationService {

    private final RecipeRepository recipes;
    private final UserRepository users;
    private final RecipeMapper mapper;
    private final Clock clock;

    public RecipeModerationService(RecipeRepository recipes, UserRepository users, RecipeMapper mapper, Clock clock) {
        this.recipes = recipes;
        this.users = users;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Cũ nhất trước để không công thức nào bị bỏ quên. */
    @Transactional(readOnly = true)
    public Page<RecipeCard> queue(RecipeStatus status, Pageable p) {
        return recipes.findByStatus(status == null ? RecipeStatus.PENDING : status,
                PageRequest.of(p.getPageNumber(), p.getPageSize(), Sort.by(Sort.Direction.ASC, "updatedAt")))
                .map(mapper::card);
    }

    @Transactional(readOnly = true)
    public RecipeDetail get(Long id) {
        return mapper.detail(find(id));
    }

    public RecipeDetail approve(AppUserDetails mod, Long id) {
        Recipe r = find(id);
        r.approve(moderator(mod), clock.instant());
        return mapper.detail(r);
    }

    public RecipeDetail reject(AppUserDetails mod, Long id, String reason) {
        Recipe r = find(id);
        r.reject(moderator(mod), reason.strip(), clock.instant());
        return mapper.detail(r);
    }

    public RecipeDetail hide(AppUserDetails mod, Long id, String reason) {
        Recipe r = find(id);
        r.hide(moderator(mod), reason.strip(), clock.instant());
        return mapper.detail(r);
    }

    public RecipeDetail unhide(AppUserDetails mod, Long id) {
        Recipe r = find(id);
        r.unhide(moderator(mod), clock.instant());
        return mapper.detail(r);
    }

    private Recipe find(Long id) {
        return recipes.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
    }

    private User moderator(AppUserDetails mod) {
        return users.getReferenceById(mod.id());
    }
}
