package com.recipehub.recipe;

import com.recipehub.catalog.CategoryRepository;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.IngredientRepository;
import com.recipehub.catalog.Tag;
import com.recipehub.catalog.TagRepository;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.common.Slugs;
import com.recipehub.media.Media;
import com.recipehub.media.MediaService;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.recipe.dto.RecipeIngredientRequest;
import com.recipehub.recipe.dto.RecipeRequest;
import com.recipehub.recipe.dto.RecipeStepRequest;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.UserRepository;
import java.text.Normalizer;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Thao tác của tác giả trên công thức của chính mình. */
@Service
@Transactional
public class RecipeService {

    static final String MODERATE = "CONTENT_MODERATE";

    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final IngredientRepository ingredients;
    private final UserRepository users;
    private final MediaService media;
    private final RecipeMapper mapper;

    public RecipeService(RecipeRepository recipes, CategoryRepository categories, TagRepository tags,
                         IngredientRepository ingredients, UserRepository users, MediaService media, RecipeMapper mapper) {
        this.recipes = recipes;
        this.categories = categories;
        this.tags = tags;
        this.ingredients = ingredients;
        this.users = users;
        this.media = media;
        this.mapper = mapper;
    }

    public RecipeDetail create(AppUserDetails actor, RecipeRequest r) {
        String base = Slugs.slugify(r.title());
        if (base.isEmpty()) base = "cong-thuc";
        Recipe recipe = new Recipe(users.getReferenceById(actor.id()), r.title().strip(),
                Slugs.unique(base, recipes::existsBySlug));
        apply(recipe, r, actor.id());
        return mapper.detail(recipes.save(recipe));
    }

    public RecipeDetail update(AppUserDetails actor, Long id, RecipeRequest r) {
        Recipe recipe = own(actor, id);
        recipe.markEdited();
        apply(recipe, r, actor.id());
        if (recipe.getStatus() == RecipeStatus.PENDING) recipe.requireComplete(); // bản đã đăng sửa → chờ duyệt lại
        return mapper.detail(recipe);
    }

    public RecipeDetail submit(AppUserDetails actor, Long id) {
        Recipe recipe = own(actor, id);
        recipe.submit();
        return mapper.detail(recipe);
    }

    public void delete(AppUserDetails actor, Long id) {
        Recipe recipe = visible(actor, id);
        if (!isAuthor(recipe, actor) && !actor.can(MODERATE)) throw forbidden();
        recipes.delete(recipe);
    }

    /** Bản đầy đủ cho trình soạn; chỉ tác giả. */
    @Transactional(readOnly = true)
    public RecipeDetail getMine(AppUserDetails actor, Long id) {
        return mapper.detail(recipes.findById(id).filter(x -> isAuthor(x, actor)).orElseThrow(RecipeService::notFound));
    }

    @Transactional(readOnly = true)
    public Page<RecipeCard> listMine(AppUserDetails actor, Set<RecipeStatus> statuses, Pageable p) {
        Set<RecipeStatus> in = statuses == null || statuses.isEmpty() ? EnumSet.allOf(RecipeStatus.class) : statuses;
        Pageable page = PageRequest.of(p.getPageNumber(), p.getPageSize(), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return recipes.findByAuthor_IdAndStatusIn(actor.id(), in, page).map(mapper::card);
    }

    /** Ghi đè toàn bộ nội dung; kiểm tra hết trước khi đụng vào entity. */
    private void apply(Recipe recipe, RecipeRequest r, Long authorId) {
        List<Long> ingredientIds = r.ingredients().stream().map(RecipeIngredientRequest::ingredientId).toList();
        if (Set.copyOf(ingredientIds).size() != ingredientIds.size()) {
            throw new ApiException(ErrorCode.DUPLICATE_INGREDIENT, "Mỗi nguyên liệu chỉ được xuất hiện một lần");
        }
        Map<Long, Ingredient> byId = ingredients.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        if (byId.size() != ingredientIds.size()) throw invalid("Nguyên liệu không tồn tại");
        List<Tag> tagList = tags.findAllById(r.tagIds());
        if (tagList.size() != r.tagIds().size()) throw invalid("Tag không tồn tại");
        var category = r.categoryId() == null ? null
                : categories.findById(r.categoryId()).orElseThrow(() -> invalid("Danh mục không tồn tại"));
        Media cover = owned(r.coverMediaId(), authorId);
        List<Media> stepMedia = r.steps().stream().map(s -> owned(s.mediaId(), authorId)).toList();

        recipe.setTitle(Normalizer.normalize(r.title().strip(), Normalizer.Form.NFC)); // tìm theo tiêu đề so dạng NFC
        recipe.setSummary(r.summary() == null || r.summary().isBlank() ? null : r.summary().strip());
        recipe.setCategory(category);
        recipe.setServings(r.servings());
        recipe.setPrepMinutes(r.prepMinutes());
        recipe.setCookMinutes(r.cookMinutes());
        recipe.setDifficulty(r.difficulty());
        recipe.setCover(cover);
        recipe.getTags().clear();
        recipe.getTags().addAll(tagList);
        // Xóa rồi thêm lại: Hibernate insert trước delete, unique (recipe, ingredient)/(recipe, step_no) là DEFERRABLE.
        recipe.getIngredients().clear();
        for (RecipeIngredientRequest l : r.ingredients()) {
            recipe.addIngredient(byId.get(l.ingredientId()), l.quantity(), l.unit(), blankToNull(l.note()));
        }
        recipe.getSteps().clear();
        for (int i = 0; i < r.steps().size(); i++) {
            RecipeStepRequest s = r.steps().get(i);
            recipe.addStep(s.content().strip(), stepMedia.get(i));
        }
    }

    private Media owned(Long mediaId, Long authorId) {
        return mediaId == null ? null : media.requireOwned(mediaId, authorId);
    }

    /** Không thấy được (bản chưa đăng của người khác) → 404, như thể không tồn tại. */
    private Recipe visible(AppUserDetails actor, Long id) {
        return recipes.findById(id)
                .filter(x -> isAuthor(x, actor) || x.getStatus() == RecipeStatus.PUBLISHED || actor.can(MODERATE))
                .orElseThrow(RecipeService::notFound);
    }

    private Recipe own(AppUserDetails actor, Long id) {
        Recipe recipe = visible(actor, id);
        if (!isAuthor(recipe, actor)) throw forbidden();
        return recipe;
    }

    private static boolean isAuthor(Recipe r, AppUserDetails actor) {
        return r.getAuthor().getId().equals(actor.id());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức");
    }

    private static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải tác giả công thức này");
    }

    private static ApiException invalid(String msg) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, msg);
    }
}
