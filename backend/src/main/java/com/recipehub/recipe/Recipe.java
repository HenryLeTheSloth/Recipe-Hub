package com.recipehub.recipe;

import com.recipehub.catalog.Category;
import com.recipehub.catalog.Ingredient;
import com.recipehub.catalog.Tag;
import com.recipehub.catalog.Unit;
import com.recipehub.common.ApiException;
import com.recipehub.common.BaseEntity;
import com.recipehub.common.ErrorCode;
import com.recipehub.media.Media;
import com.recipehub.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.recipehub.recipe.RecipeStatus.DRAFT;
import static com.recipehub.recipe.RecipeStatus.HIDDEN;
import static com.recipehub.recipe.RecipeStatus.PENDING;
import static com.recipehub.recipe.RecipeStatus.PUBLISHED;
import static com.recipehub.recipe.RecipeStatus.REJECTED;

/** Công thức; mọi chuyển trạng thái (spec §5) nằm ở đây, sai chuyển → INVALID_TRANSITION. */
@Entity
@Table(name = "recipes")
public class Recipe extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    private String title;
    private String slug;
    private String summary;
    private int servings = 1;
    private int prepMinutes;
    private int cookMinutes;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty = Difficulty.EASY;

    @Enumerated(EnumType.STRING)
    private RecipeStatus status = DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_id")
    private Media cover;

    @ManyToMany
    @JoinTable(name = "recipe_tags", joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo")
    private List<RecipeStep> steps = new ArrayList<>();

    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private Instant reviewedAt;
    private Instant publishedAt;

    protected Recipe() {
    }

    public Recipe(User author, String title, String slug) {
        this.author = author;
        this.title = title;
        this.slug = slug;
    }

    /** Thêm dòng nguyên liệu cuối danh sách (position = chỉ số). */
    public void addIngredient(Ingredient ingredient, BigDecimal quantity, Unit unit, String note) {
        ingredients.add(new RecipeIngredient(this, ingredient, quantity, unit, note, ingredients.size()));
    }

    /** Thêm bước cuối danh sách (stepNo bắt đầu từ 1). */
    public void addStep(String content, Media media) {
        steps.add(new RecipeStep(this, steps.size() + 1, content, media));
    }

    /** Phần còn thiếu để gửi duyệt, theo thứ tự cố định. */
    public List<String> missingForSubmit() {
        List<String> missing = new ArrayList<>();
        if (category == null) missing.add("danh mục");
        if (ingredients.isEmpty()) missing.add("nguyên liệu");
        if (steps.isEmpty()) missing.add("các bước");
        return missing;
    }

    public void submit() {
        require(status == DRAFT || status == REJECTED, "Chỉ gửi duyệt được bản nháp hoặc bản bị từ chối");
        requireComplete();
        status = PENDING;
        rejectionReason = null;
    }

    /** Thiếu phần bắt buộc → RECIPE_INCOMPLETE "Còn thiếu: …". */
    public void requireComplete() {
        List<String> missing = missingForSubmit();
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.RECIPE_INCOMPLETE, "Còn thiếu: " + String.join(", ", missing));
        }
    }

    /** Gọi trước khi tác giả lưu thay đổi: bản đã đăng phải được duyệt lại. */
    public void markEdited() {
        require(status != PENDING && status != HIDDEN, "Không sửa được công thức đang chờ duyệt hoặc đã bị ẩn");
        if (status == PUBLISHED) status = PENDING;
        touch(); // sửa chỉ bước/nguyên liệu không làm dòng recipes bẩn → @UpdateTimestamp không tự chạy
    }

    public void approve(User moderator, Instant now) {
        require(status == PENDING, "Chỉ duyệt được công thức đang chờ duyệt");
        status = PUBLISHED;
        rejectionReason = null;
        if (publishedAt == null) publishedAt = now;
        reviewed(moderator, now);
    }

    public void reject(User moderator, String reason, Instant now) {
        require(status == PENDING, "Chỉ từ chối được công thức đang chờ duyệt");
        status = REJECTED;
        rejectionReason = reason;
        reviewed(moderator, now);
    }

    public void hide(User moderator, String reason, Instant now) {
        require(status == PUBLISHED, "Chỉ ẩn được công thức đã đăng");
        status = HIDDEN;
        rejectionReason = reason;
        reviewed(moderator, now);
    }

    public void unhide(User moderator, Instant now) {
        require(status == HIDDEN, "Chỉ hiện lại được công thức đang bị ẩn");
        status = PUBLISHED;
        rejectionReason = null;
        reviewed(moderator, now);
    }

    private void reviewed(User moderator, Instant now) {
        reviewedBy = moderator;
        reviewedAt = now;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new ApiException(ErrorCode.INVALID_TRANSITION, message);
    }

    public User getAuthor() { return author; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getSummary() { return summary; }
    public int getServings() { return servings; }
    public int getPrepMinutes() { return prepMinutes; }
    public int getCookMinutes() { return cookMinutes; }
    public Difficulty getDifficulty() { return difficulty; }
    public RecipeStatus getStatus() { return status; }
    public Media getCover() { return cover; }
    public Set<Tag> getTags() { return tags; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
    public String getRejectionReason() { return rejectionReason; }
    public User getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void setCategory(Category category) { this.category = category; }
    public void setTitle(String title) { this.title = title; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setServings(int servings) { this.servings = servings; }
    public void setPrepMinutes(int prepMinutes) { this.prepMinutes = prepMinutes; }
    public void setCookMinutes(int cookMinutes) { this.cookMinutes = cookMinutes; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setCover(Media cover) { this.cover = cover; }
}
