# Recipe Hub — Giai đoạn 4: Cộng đồng — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chấm điểm 1–5 sao kèm nhận xét, yêu thích, bình luận trả lời 1 cấp; moderator ẩn đánh giá/bình luận; điểm trung bình hiện trên thẻ và trang chi tiết, sắp xếp theo điểm.

**Architecture:** Package `community` với 3 service nhỏ. Điểm trung bình không lưu sẵn: một truy vấn gom nhóm cho mọi công thức trong trang kết quả. Mọi tương tác yêu cầu công thức PUBLISHED.

**Tech Stack:** Spring Boot 4.2, Data JPA, Flyway; Vue 3, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md` (§4, §6)

## Global Constraints

- Java 25, Spring Boot 4.2.0. Không Lombok; DTO là `record`.
- Boot 4: `@MockitoBean`, Jackson 3; test slice ở starter test riêng — IDE auto-import.
- Schema chỉ qua Flyway; `ddl-auto=validate`.
- Lỗi API: `ApiException(ErrorCode.X, "…")`; giai đoạn này thêm `COMMENT_DEPTH(400)`.
- Giới hạn: stars 1–5, review ≤1000, comment 1–1000 ký tự; danh sách bình luận tối đa 200 bình luận gốc (cũ nhất trước).
- Tham gia (chấm, bình luận, yêu thích) cần `COMMUNITY_PARTICIPATE`; ẩn/hiện cần `CONTENT_MODERATE`.
- Cổng: `./mvnw verify` và `npm run test && npm run type-check` xanh 100%.

## Review Focus

1. Tác giả tự chấm công thức của mình → 403. → Task 1
2. Chấm/bình luận/yêu thích công thức không PUBLISHED (DRAFT, HIDDEN…) → 404. → Task 1, Task 2
3. Trả lời một câu trả lời → 400 `COMMENT_DEPTH`. → Task 2
4. Đánh giá bị ẩn không tính vào trung bình/số lượt; bình luận bị ẩn (và các trả lời của nó) không hiện với member nhưng hiện với moderator kèm `hidden: true`. → Task 1, Task 2
5. Admin xóa user có bình luận/đánh giá/công thức → 409 `USER_HAS_CONTENT`; user chỉ có yêu thích → xóa được, yêu thích mất theo. → Task 2

---

### Task 1: Đánh giá + điểm trung bình

**Files:**
- Create: `db/migration/V8__community.sql` (ratings, favorites, comments theo spec §4)
- Create: `com/recipehub/community/{Rating,RatingRepository,RatingService,RatingController,AdminRatingController,RatingStats}.java`, `dto/{RatingRequest,RatingView,HiddenRequest}.java`
- Modify: `recipe/dto/RecipeCard.java`, `recipe/dto/RecipeDetail.java` (+`Double avgRating`, `long ratingCount`), `recipe/RecipeQueryService.java` (+`sort=rating`), `recipe/RecipeMapper.java`
- Test: `community/RatingServiceTest.java`, `community/RatingRepositoryTest.java`, `recipe/RecipeQueryServiceIT.java` (thêm case)

**Interfaces:**
- Consumes: `RecipeQueryService` (GĐ3), `AppUserDetails`.
- Produces:
  ```java
  public record RatingRequest(@Min(1) @Max(5) int stars, @Size(max=1000) String review)
  public record RatingView(Long id, int stars, String review, AuthorView author, boolean hidden, Instant updatedAt)
  public record HiddenRequest(boolean hidden)
  public record RatingStats(Long recipeId, Double avg, long count)   // avg làm tròn 1 chữ số; null khi chưa có đánh giá
  // RatingRepository
  @Query("select new …RatingStats(r.recipe.id, avg(r.stars), count(r)) from Rating r where r.recipe.id in :ids and r.hidden = false group by r.recipe.id")
  List<RatingStats> statsFor(Collection<Long> ids);
  Optional<Rating> findByUser_IdAndRecipe_Id(Long userId, Long recipeId);
  public class RatingService {
    RatingView upsert(AppUserDetails actor, Long recipeId, RatingRequest r);    // lần 2 cập nhật, không tạo mới
    void deleteMine(AppUserDetails actor, Long recipeId);
    Page<RatingView> list(Long recipeId, @Nullable AppUserDetails viewer, Pageable p); // ẩn chỉ hiện với CONTENT_MODERATE
    RatingView setHidden(Long ratingId, boolean hidden);
  }
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | GET `/api/recipes/{id}/ratings?page&size` | công khai | Page<RatingView> |
  | PUT `/api/recipes/{id}/my-rating` | COMMUNITY_PARTICIPATE | 200 RatingView |
  | DELETE `/api/recipes/{id}/my-rating` | COMMUNITY_PARTICIPATE | 204 |
  | PATCH `/api/admin/ratings/{id}` `{hidden}` | CONTENT_MODERATE | 200 RatingView |
- `sort=rating`: theo avg giảm dần, công thức chưa có đánh giá xếp cuối, hòa thì `publishedAt` mới hơn trước.

- [ ] **Step 1: Viết test fail**

```java
// RatingServiceTest (Mockito)
@Test void authorCannotRateOwnRecipe() { /* FORBIDDEN */ }
@Test void cannotRateUnpublished() { /* recipe HIDDEN → NOT_FOUND */ }
@Test void secondRatingUpdatesExisting() { /* findByUser_IdAndRecipe_Id trả rating cũ → stars cập nhật, save cùng id */ }

// RatingRepositoryTest — @DataJpaTest + Testcontainers
@Test void statsIgnoreHidden() { /* 5★, 3★, 1★(hidden) → avg 4.0, count 2 */ }
@Test void oneRatingPerUserPerRecipe() { /* insert trùng + flush → DataIntegrityViolationException */ }
@Test void starsOutOfRangeRejectedByDb() { /* stars 6 → DataIntegrityViolationException (CHECK) */ }

// RecipeQueryServiceIT (thêm)
@Test void sortByRatingPutsUnratedLast() { /* A avg 4.5, B avg 3, C chưa có → [A, B, C] */ }
@Test void cardsCarryAverage() { /* RecipeCard.avgRating 4.5, ratingCount 2 */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='Rating*Test,RecipeQueryServiceIT'` — Expected: FAIL.
- [ ] **Step 3: Implement.** `RecipeQueryService.search`: lấy trang → `statsFor(ids)` một lần → ghép vào card. Sắp theo rating bằng Specification + `orderBy` trên subquery avg (hoặc `@Query` riêng cho nhánh `sort=rating`).
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(community): ratings with averages and rating sort"`

---

### Task 2: Yêu thích + bình luận + xóa user có nội dung

**Files:**
- Create: `com/recipehub/community/{Favorite,FavoriteId,FavoriteRepository,FavoriteService,FavoriteController,Comment,CommentRepository,CommentService,CommentController,AdminCommentController}.java`, `dto/{CommentRequest,CommentView}.java`
- Modify: `recipe/dto/RecipeDetail.java` (+`boolean favorited` — false khi chưa đăng nhập), `recipe/RecipeQueryService.java`
- Test: `community/CommentServiceTest.java`, `community/FavoriteServiceTest.java`, `community/CommunityControllerTest.java`, `user/UserDeletionIT.java`

**Interfaces:**
- Produces:
  ```java
  public record CommentRequest(@NotBlank @Size(max=1000) String content, Long parentId)
  public record CommentView(Long id, String content, AuthorView author, boolean hidden, Instant createdAt, List<CommentView> replies)
                                                                      // replies luôn rỗng ở cấp trả lời
  public class CommentService {
    List<CommentView> list(Long recipeId, @Nullable AppUserDetails viewer); // gốc cũ nhất trước, tối đa 200; trả lời cũ nhất trước
    CommentView create(AppUserDetails actor, Long recipeId, CommentRequest r); // parent phải cùng recipe, parent.parent != null → COMMENT_DEPTH
    void delete(AppUserDetails actor, Long commentId);                       // tác giả hoặc CONTENT_MODERATE; xóa theo các trả lời
    CommentView setHidden(Long commentId, boolean hidden);
  }
  public class FavoriteService {
    void add(AppUserDetails actor, Long recipeId);      // idempotent
    void remove(AppUserDetails actor, Long recipeId);   // idempotent
    Page<RecipeCard> listMine(AppUserDetails actor, Pageable p);  // chỉ PUBLISHED, mới thích nhất trước
    boolean isFavorite(Long userId, Long recipeId);
  }
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | GET `/api/recipes/{id}/comments` | công khai | List<CommentView> |
  | POST `/api/recipes/{id}/comments` | COMMUNITY_PARTICIPATE | 201 CommentView |
  | DELETE `/api/comments/{id}` | đăng nhập | 204 |
  | PATCH `/api/admin/comments/{id}` `{hidden}` | CONTENT_MODERATE | 200 CommentView |
  | PUT / DELETE `/api/recipes/{id}/favorite` | COMMUNITY_PARTICIPATE | 204 |
  | GET `/api/me/favorites?page&size` | đăng nhập | Page<RecipeCard> |
- Hiển thị: viewer không có `CONTENT_MODERATE` → bỏ bình luận `hidden` và toàn bộ trả lời của bình luận gốc bị ẩn.

- [ ] **Step 1: Viết test fail**

```java
// CommentServiceTest (Mockito)
@Test void replyToReplyRejected() { /* parent.parent != null → COMMENT_DEPTH */ }
@Test void parentMustBelongToSameRecipe() { /* VALIDATION_ERROR */ }
@Test void cannotCommentOnUnpublished() { /* NOT_FOUND */ }
@Test void hiddenInvisibleToMembersVisibleToModerators() {
  // gốc G1 ẩn có trả lời R1; gốc G2 hiện → member thấy [G2]; moderator thấy [G1(hidden=true, replies=[R1]), G2]
}
@Test void othersCommentCannotBeDeletedByMember() { /* FORBIDDEN */ }

// FavoriteServiceTest
@Test void addTwiceIsIdempotent() {}
@Test void cannotFavoriteUnpublished() { /* NOT_FOUND */ }

// CommunityControllerTest — @WebMvcTest
@Test void anonymousCanReadComments() {}
@Test @WithMockUser(authorities = "RECIPE_WRITE") void commentingNeedsCommunityPermission() { /* 403 */ }

// UserDeletionIT — @SpringBootTest + Testcontainers, admin gọi DELETE /api/admin/users/{id}
@Test void userWithCommentCannotBeDeleted() { /* 409 USER_HAS_CONTENT */ }
@Test void userWithRatingCannotBeDeleted() { /* 409 USER_HAS_CONTENT */ }
@Test void userWithOnlyFavoritesIsDeleted() { /* 204; count favorites của user = 0 */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='CommentServiceTest,FavoriteServiceTest,CommunityControllerTest,UserDeletionIT'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(community): favorites and one-level comments"`

---

### Task 3: Frontend — khu vực cộng đồng trên trang chi tiết

**Files:**
- Create: `frontend/src/api/community.ts`, `src/components/{StarRating,RatingSection,FavoriteButton,CommentSection}.vue`, `src/pages/MyFavoritesPage.vue`
- Modify: `src/pages/RecipeDetailPage.vue`, `src/components/RecipeCard.vue` (hiện ★ trung bình và số lượt), `src/pages/RecipeListPage.vue` (sắp xếp "Mới nhất" / "Điểm cao"), `src/router/index.ts` (`/me/favorites`, `meta.auth`), `src/api/messages.ts` (`COMMENT_DEPTH`)
- Test: `src/components/__tests__/{StarRating,FavoriteButton,CommentSection,RatingSection}.spec.ts`

**Interfaces:**
- `StarRating.vue`: props `{ modelValue: number; readonly?: boolean }`; nhóm radio `role="radiogroup"`, mỗi sao `role="radio"` + `aria-label="3 sao"`; phím ←/→ đổi giá trị.
- `FavoriteButton.vue`: props `{ recipeId: number; favorited: boolean }`; đổi trạng thái ngay (optimistic), API lỗi → trả về trạng thái cũ + toast lỗi. Chưa đăng nhập → chuyển `/login?redirect=…`.
- `CommentSection.vue`: nút "Trả lời" chỉ có ở bình luận gốc; moderator thấy nút "Ẩn"/"Hiện" và nhãn "Đã ẩn".
- `RatingSection.vue`: không hiện form chấm điểm khi viewer là tác giả.

- [ ] **Step 1: Viết test fail**

```ts
it('StarRating changes with arrow keys', async () => { /* value 3, ArrowRight → emit 4; ArrowLeft ở 1 → giữ 1 */ })
it('FavoriteButton rolls back on error', async () => { /* favorited false, click → hiển thị đã thích, API reject → về chưa thích */ })
it('FavoriteButton redirects anonymous to login', async () => {})
it('reply button only on root comments', () => { /* gốc có 1 trả lời → đúng 1 nút "Trả lời" */ })
it('moderator sees hide buttons', () => {})
it('author does not see rating form', () => {})
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Cổng giai đoạn** — Run: `cd backend && ./mvnw verify && cd ../frontend && npm run test && npm run type-check` — Expected: tất cả xanh.
- [ ] **Step 5: Kiểm tra tay** — 2 member chấm điểm một công thức → trung bình đúng; moderator ẩn 1 đánh giá → trung bình đổi.
- [ ] **Step 6: Commit** — `git commit -am "feat(frontend): ratings, favorites and comments"`
