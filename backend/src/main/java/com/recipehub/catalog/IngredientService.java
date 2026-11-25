package com.recipehub.catalog;

import com.recipehub.catalog.dto.IngredientRequest;
import com.recipehub.catalog.dto.IngredientResponse;
import com.recipehub.catalog.dto.SuggestResult;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.common.Slugs;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.recipehub.catalog.IngredientStatus.APPROVED;
import static com.recipehub.catalog.IngredientStatus.PENDING;

@Service
public class IngredientService {

    static final Limit MAX_LIST = Limit.of(200);

    private final IngredientRepository repo;

    public IngredientService(IngredientRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> listApproved(String q, IngredientGroup group) {
        String s = Names.clean(q);
        return repo.search(s, Slugs.slugify(s), group, APPROVED, MAX_LIST).stream()
                .map(IngredientService::response).toList();
    }

    /** Trùng tên (trim, không phân biệt hoa thường) → trả cái có sẵn; không thì tạo PENDING.
     *  Trùng slug thì không coi là trùng ("Dừa" ≠ "Dứa") — slug thêm -2 theo spec. */
    @Transactional
    public SuggestResult suggest(Long userId, IngredientRequest r) {
        String name = Names.clean(r.name());
        String base = Names.baseSlug(name);
        var existing = repo.findByNameIgnoreCase(name);
        if (existing.isPresent()) return new SuggestResult(response(existing.get()), false);
        String slug = Names.uniqueSlug(base, repo::findBySlug, null);
        // ponytail: hai người đề xuất cùng lúc → người sau nhận 409 NAME_TAKEN, thử lại sẽ nhận bản có sẵn
        Ingredient i = new Ingredient(name, slug, r.group(), emoji(r.emoji()), PENDING, userId);
        return new SuggestResult(response(Names.save(() -> repo.saveAndFlush(i))), true);
    }

    @Transactional(readOnly = true)
    public Page<IngredientResponse> adminList(IngredientStatus status, String q, Pageable p) {
        String s = Names.clean(q);
        return repo.adminSearch(status, s, Slugs.slugify(s), p).map(IngredientService::response);
    }

    @Transactional
    public IngredientResponse update(Long id, IngredientRequest r, IngredientStatus status) {
        Ingredient i = repo.findById(id).orElseThrow(Names::notFound);
        String name = Names.clean(r.name());
        String base = Names.baseSlug(name);
        Names.requireFree(repo.findByNameIgnoreCase(name), id);
        i.setName(name);
        i.setSlug(Names.uniqueSlug(base, repo::findBySlug, id));
        i.setIngredientGroup(r.group());
        i.setEmoji(emoji(r.emoji()));
        i.setStatus(status);
        return response(Names.save(() -> repo.saveAndFlush(i)));
    }

    @Transactional
    public void delete(Long id) {
        Ingredient i = repo.findById(id).orElseThrow(Names::notFound);
        Names.delete(() -> {
            repo.delete(i);
            repo.flush();
        });
    }

    private static String emoji(String e) {
        return e == null || e.isBlank() ? null : e.trim();
    }

    static IngredientResponse response(Ingredient i) {
        return new IngredientResponse(i.getId(), i.getName(), i.getSlug(), i.getIngredientGroup(), i.getEmoji(),
                i.getStatus());
    }

    /** Gộp source vào target (trùng tên/biến thể); mọi công thức dùng source chuyển sang target. */
    @Transactional
    public void merge(Long sourceId, Long targetId) {
        if (sourceId.equals(targetId)) throw new ApiException(ErrorCode.VALIDATION_ERROR, "Không thể gộp vào chính nó");
        if (!repo.existsById(sourceId) || !repo.existsById(targetId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên liệu");
        }
        repo.dropRecipeLinesDuplicatedByTarget(sourceId, targetId);
        repo.repointRecipeLines(sourceId, targetId);
        repo.deleteNative(sourceId);
    }
}
