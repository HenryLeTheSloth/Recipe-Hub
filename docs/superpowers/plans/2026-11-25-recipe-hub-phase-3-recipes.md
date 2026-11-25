# Recipe Hub — Giai đoạn 3: Công thức — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thành viên viết công thức (nguyên liệu có định lượng, các bước, ảnh, tag), gửi duyệt; moderator duyệt/từ chối/ẩn; khách xem và tìm kiếm (cả theo bộ nguyên liệu); moderator gộp nguyên liệu trùng.

**Architecture:** Trạng thái công thức là các method trên entity `Recipe` (test thuần, không cần Spring). Ba service: `RecipeService` (tác giả), `RecipeModerationService`, `RecipeQueryService` (đọc công khai, JPA Specification). Ràng buộc unique của bảng con là DEFERRABLE để thay toàn bộ danh sách khi sửa.

**Tech Stack:** Spring Boot 4.2, Data JPA (Specification), MapStruct, Flyway; Vue 3, PrimeVue, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md` (§4, §5, §9)

## Global Constraints

- Java 25, Spring Boot 4.2.0, MapStruct 1.6.x. Không Lombok; DTO là `record`.
- Boot 4: `@MockitoBean`, Jackson 3; test slice ở starter test riêng — IDE auto-import.
- Package `com.recipehub.<feature>`. Schema chỉ qua Flyway; `ddl-auto=validate`.
- Lỗi API: `ApiException(ErrorCode.X, "…")`; giai đoạn này thêm `INVALID_TRANSITION(409)`, `RECIPE_INCOMPLETE(400)`, `DUPLICATE_INGREDIENT(400)`.
- Giới hạn (spec): title 3–120, summary ≤500, servings 1–50, prep/cook 0–1440 phút, ≤50 nguyên liệu, quantity >0 và ≤100000 (`NUMERIC(10,2)`), note ≤100, ≤30 bước, nội dung bước 1–2000, lý do từ chối/ẩn 1–500.
- Slug công thức tạo **một lần** lúc tạo, không đổi khi sửa tiêu đề.
- Cổng: `./mvnw verify` và `npm run test && npm run type-check` xanh 100%.

## Review Focus

1. Người không phải tác giả (kể cả khách) xem slug công thức DRAFT/PENDING/REJECTED/HIDDEN → 404, không phải 403. → Task 4
2. Tác giả sửa công thức PUBLISHED → chuyển PENDING và biến mất khỏi danh sách công khai cho tới khi được duyệt lại; `publishedAt` gốc giữ nguyên. → Task 2, Task 3
3. Gửi duyệt khi thiếu danh mục / nguyên liệu / bước → 400 `RECIPE_INCOMPLETE`, `detail` liệt kê đúng phần thiếu. → Task 1
4. Tìm `ingredientIds=[trứng, cà chua]` → chỉ công thức có **đủ cả hai**, không trùng lặp, `totalElements` đúng. → Task 4
5. Hai công thức cùng tiêu đề → slug `ca-chua-xao-trung` và `ca-chua-xao-trung-2`; sửa danh sách bước 1..3 thành 1..2 với nội dung khác không vỡ unique constraint. → Task 2

---

### Task 1: Schema, entity, máy trạng thái

**Files:**
- Create: `db/migration/V7__recipes.sql` (recipes, recipe_tags, recipe_ingredients, recipe_steps theo spec §4; `UNIQUE (recipe_id, ingredient_id)` và `UNIQUE (recipe_id, step_no)` khai báo `DEFERRABLE INITIALLY DEFERRED`; CHECK servings 1–50)
- Create: `com/recipehub/catalog/Unit.java`, `com/recipehub/recipe/{Recipe,RecipeStatus,Difficulty,RecipeIngredient,RecipeStep,RecipeRepository}.java`
- Test: `recipe/RecipeTransitionsTest.java`, `recipe/RecipeRepositoryTest.java`

**Interfaces:**
- Produces:
  ```java
  public enum Unit { G, KG, ML, L, TSP, TBSP, CUP, PIECE, PINCH }
  public class Recipe extends BaseEntity {
    // author, category (nullable), title, slug, summary, servings, prepMinutes, cookMinutes, difficulty, status,
    // cover (Media), tags (Set<Tag>), ingredients (List, orderBy position, cascade ALL, orphanRemoval),
    // steps (List, orderBy stepNo, cascade ALL, orphanRemoval), rejectionReason, reviewedBy, reviewedAt, publishedAt
    List<String> missingForSubmit();               // ["danh mục", "nguyên liệu", "các bước"] theo thứ tự đó
    void submit();                                  // DRAFT|REJECTED → PENDING; thiếu → RECIPE_INCOMPLETE "Còn thiếu: danh mục, các bước"
    void markEdited();                              // DRAFT|REJECTED giữ nguyên; PUBLISHED → PENDING; PENDING|HIDDEN → INVALID_TRANSITION
    void approve(User moderator, Instant now);      // PENDING → PUBLISHED; publishedAt chỉ set khi đang null; xóa rejectionReason
    void reject(User moderator, String reason, Instant now);  // PENDING → REJECTED
    void hide(User moderator, String reason, Instant now);    // PUBLISHED → HIDDEN (lưu reason vào rejectionReason)
    void unhide(User moderator, Instant now);                  // HIDDEN → PUBLISHED
  }
  // RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe>
  Optional<Recipe> findBySlug(String slug); boolean existsBySlug(String slug);
  Page<Recipe> findByAuthor_IdAndStatusIn(Long authorId, Collection<RecipeStatus> s, Pageable p);
  Page<Recipe> findByStatus(RecipeStatus s, Pageable p);
  ```

- [ ] **Step 1: Viết test fail**

```java
// RecipeTransitionsTest — thuần Java, không Spring
@ParameterizedTest @CsvSource({ "DRAFT,PENDING", "REJECTED,PENDING" })
void submitAllowed(RecipeStatus from, RecipeStatus to) { /* recipe đủ dữ liệu → submit() → status == to */ }
@ParameterizedTest @EnumSource(names = { "PENDING", "PUBLISHED", "HIDDEN" })
void submitDenied(RecipeStatus from) { /* INVALID_TRANSITION */ }
@Test void submitIncompleteListsMissing() {
  // không category, có nguyên liệu, không bước → RECIPE_INCOMPLETE, message "Còn thiếu: danh mục, các bước"
}
@Test void editingPublishedGoesBackToPending() {}
@ParameterizedTest @EnumSource(names = { "PENDING", "HIDDEN" }) void editingDenied(RecipeStatus s) {}
@Test void approveKeepsOriginalPublishedAt() { /* approve t1, markEdited, approve t2 → publishedAt == t1 */ }
@Test void approveOnlyFromPending() {}
@Test void hideOnlyFromPublished() {} @Test void unhideOnlyFromHidden() {}

// RecipeRepositoryTest — @DataJpaTest + Testcontainers
@Test void deletingRecipeCascadesChildren() { /* sau delete+flush: count recipe_ingredients, recipe_steps, recipe_tags = 0 */ }
@Test void categoryInUseCannotBeDeleted() { /* DataIntegrityViolationException */ }
@Test void authorWithRecipeCannotBeDeleted() { /* DataIntegrityViolationException */ }
@Test void ingredientInUseCannotBeDeleted() { /* DataIntegrityViolationException */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='RecipeTransitionsTest,RecipeRepositoryTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(recipe): schema, entities and status transitions"`

---

### Task 2: Thao tác của tác giả

**Files:**
- Create: `com/recipehub/recipe/{RecipeService,RecipeMapper,RecipeController,MyRecipeController}.java`, `dto/{RecipeRequest,RecipeIngredientRequest,RecipeStepRequest,RecipeDetail,RecipeCard}.java`
- Test: `recipe/RecipeServiceTest.java`, `recipe/RecipeServiceIT.java`, `recipe/RecipeControllerTest.java`

**Interfaces:**
- Consumes: `AppUserDetails` (id, `can()`), `MediaService.requireOwned`, `Slugs.unique`, repository danh mục/tag/nguyên liệu.
- Produces:
  ```java
  public record RecipeRequest(@NotBlank @Size(min=3,max=120) String title, @Size(max=500) String summary, Long categoryId,
      @Min(1) @Max(50) int servings, @Min(0) @Max(1440) int prepMinutes, @Min(0) @Max(1440) int cookMinutes,
      @NotNull Difficulty difficulty, Long coverMediaId, @NotNull Set<Long> tagIds,
      @NotNull @Size(max=50) @Valid List<RecipeIngredientRequest> ingredients, @NotNull @Size(max=30) @Valid List<RecipeStepRequest> steps)
  public record RecipeIngredientRequest(@NotNull Long ingredientId, @NotNull @DecimalMin(value="0", inclusive=false) @DecimalMax("100000") BigDecimal quantity,
      @NotNull Unit unit, @Size(max=100) String note)            // position = index trong list
  public record RecipeStepRequest(@NotBlank @Size(max=2000) String content, Long mediaId)   // stepNo = index + 1
  public record RecipeDetail(Long id, String slug, String title, String summary, CategoryResponse category, List<TagResponse> tags,
      int servings, int prepMinutes, int cookMinutes, Difficulty difficulty, RecipeStatus status, String coverUrl, AuthorView author,
      List<IngredientLine> ingredients, List<StepView> steps, String rejectionReason, Instant publishedAt, Instant createdAt, Instant updatedAt)
    public record AuthorView(Long id, String username, String displayName, String avatarUrl)
    public record IngredientLine(Long ingredientId, String name, String emoji, BigDecimal quantity, Unit unit, String note)
    public record StepView(int stepNo, String content, String imageUrl)
  public record RecipeCard(Long id, String slug, String title, String summary, String coverUrl, String categoryName,
      Difficulty difficulty, int totalMinutes, String authorName, RecipeStatus status, Instant publishedAt)
  public class RecipeService {
    RecipeDetail create(AppUserDetails actor, RecipeRequest r);                 // DRAFT
    RecipeDetail update(AppUserDetails actor, Long id, RecipeRequest r);         // markEdited() rồi thay toàn bộ tag/nguyên liệu/bước
    void delete(AppUserDetails actor, Long id);                                  // tác giả hoặc CONTENT_MODERATE
    RecipeDetail submit(AppUserDetails actor, Long id);
    Page<RecipeCard> listMine(AppUserDetails actor, Set<RecipeStatus> statuses, Pageable p); // statuses rỗng = tất cả
  }
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | POST `/api/recipes` | RECIPE_WRITE | 201 RecipeDetail |
  | PUT `/api/recipes/{id}` | RECIPE_WRITE | 200 RecipeDetail |
  | DELETE `/api/recipes/{id}` | đăng nhập | 204 |
  | POST `/api/recipes/{id}/submit` | RECIPE_WRITE | 200 RecipeDetail |
  | GET `/api/me/recipes?status&page&size` | đăng nhập | Page<RecipeCard> |
- Luật: không tìm thấy hoặc actor không được thấy → `NOT_FOUND`; thấy được (PUBLISHED) nhưng không phải tác giả → `FORBIDDEN` khi sửa/gửi duyệt. `ingredientId` lặp trong request → `DUPLICATE_INGREDIENT`. categoryId/tagId/ingredientId không tồn tại → `VALIDATION_ERROR`. coverMediaId/mediaId của bước phải thuộc tác giả (`requireOwned`).

- [ ] **Step 1: Viết test fail**

```java
// RecipeServiceTest (Mockito)
@Test void createStartsAsDraftWithUniqueSlug() { /* existsBySlug("ca-chua-xao-trung") true → slug "ca-chua-xao-trung-2" */ }
@Test void duplicateIngredientRejected() { /* 2 dòng cùng ingredientId 5 → DUPLICATE_INGREDIENT */ }
@Test void othersDraftIsNotFound() { /* actor khác, status DRAFT → NOT_FOUND */ }
@Test void othersPublishedIsForbiddenToEdit() { /* FORBIDDEN */ }
@Test void moderatorCanDeleteOthersRecipe() { /* actor.can(CONTENT_MODERATE) → delete được */ }
@Test void coverMustBeOwned() { /* requireOwned ném FORBIDDEN → lan ra */ }
@Test void slugDoesNotChangeOnTitleEdit() {}

// RecipeServiceIT — @SpringBootTest + Testcontainers, KHÔNG @Transactional (để commit thật kiểm tra DEFERRABLE)
@Test void replacingStepsAndIngredientsCommits() {
  // tạo với bước [A,B,C], nguyên liệu [trứng, cà chua] → update với bước [X,Y], nguyên liệu [cà chua, hành lá]
  // → đọc lại: steps (1,"X"),(2,"Y"); ingredients theo position [cà chua, hành lá]
}
@Test void editingPublishedRecipeReturnsToPending() { /* approve → update → status PENDING, publishedAt không đổi */ }

// RecipeControllerTest — @WebMvcTest
@Test void createRequiresRecipeWrite() { /* @WithMockUser(authorities="COMMUNITY_PARTICIPATE") → 403 */ }
@Test void validationErrors() { /* title "ab", servings 0 → 400, errors có title, servings */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='RecipeService*,RecipeControllerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.** `RecipeMapper` (MapStruct) entity → `RecipeDetail`/`RecipeCard`, URL ảnh qua `MediaService.url`.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(recipe): author create, edit, submit and delete"`

---

### Task 3: Kiểm duyệt

**Files:**
- Create: `com/recipehub/recipe/{RecipeModerationService,AdminRecipeController}.java`, `dto/ReasonRequest.java`
- Test: `recipe/RecipeModerationServiceTest.java`, `recipe/AdminRecipeControllerTest.java`

**Interfaces:**
- Produces:
  ```java
  public record ReasonRequest(@NotBlank @Size(max=500) String reason)
  public class RecipeModerationService {
    Page<RecipeCard> queue(RecipeStatus status, Pageable p);   // mặc định PENDING, cũ nhất trước (updatedAt asc)
    RecipeDetail get(Long id);                                 // moderator xem mọi trạng thái
    RecipeDetail approve(AppUserDetails mod, Long id);
    RecipeDetail reject(AppUserDetails mod, Long id, String reason);
    RecipeDetail hide(AppUserDetails mod, Long id, String reason);
    RecipeDetail unhide(AppUserDetails mod, Long id);
  }
  ```
  Endpoint (tất cả `CONTENT_MODERATE`): GET `/api/admin/recipes?status&page&size`, GET `/api/admin/recipes/{id}`, POST `/api/admin/recipes/{id}/{approve|reject|hide|unhide}` (reject/hide nhận `ReasonRequest`).

- [ ] **Step 1: Viết test fail**

```java
@Test void approveUsesClockAndModerator() { /* Clock.fixed → publishedAt, reviewedAt = now; reviewedBy = mod */ }
@Test void approveNonPendingIs409() { /* INVALID_TRANSITION */ }
@Test void rejectStoresReason() {}
@Test void queueOldestFirst() { /* Pageable sort updatedAt ASC được truyền xuống repository */ }
// AdminRecipeControllerTest
@Test @WithMockUser(authorities = "RECIPE_WRITE") void memberCannotModerate() { /* 403 */ }
@Test @WithMockUser(authorities = "CONTENT_MODERATE") void blankReasonIs400() { /* reject {"reason":" "} → 400 VALIDATION_ERROR */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='RecipeModeration*,AdminRecipeControllerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(recipe): moderation workflow"`

---

### Task 4: Đọc công khai & tìm kiếm

**Files:**
- Create: `com/recipehub/recipe/{RecipeQueryService,RecipeSpecs,PublicRecipeController}.java`, `dto/RecipeSearch.java`
- Modify: `config/SecurityConfig.java` (permitAll `GET /api/recipes`, `GET /api/recipes/**` — phủ luôn `/ratings`, `/comments` của GĐ4)
- Test: `recipe/RecipeQueryServiceIT.java`, `recipe/PublicRecipeControllerTest.java`

**Interfaces:**
- Produces:
  ```java
  public record RecipeSearch(String q, String category, String tag, List<Long> ingredientIds, String sort) // sort: "newest" (mặc định)
  public class RecipeQueryService {
    Page<RecipeCard> search(RecipeSearch s, Pageable p);               // chỉ PUBLISHED
    RecipeDetail getBySlug(String slug, @Nullable AppUserDetails viewer); // không PUBLISHED: chỉ tác giả hoặc CONTENT_MODERATE, còn lại NOT_FOUND
  }
  // RecipeSpecs (static Specification<Recipe>): published(), titleMatches(q, slugify(q)), inCategory(slug), hasTag(slug),
  //   containsAllIngredients(ids)  — subquery: recipe.id IN (select ri.recipe.id … where ri.ingredient.id in :ids
  //                                  group by ri.recipe.id having count(distinct ri.ingredient.id) = :ids.size())
  ```
  | Method & path | Kết quả |
  |---|---|
  | GET `/api/recipes?q&category&tag&ingredientIds=1,2&sort&page&size` | Page<RecipeCard> (size mặc định 12, tối đa 50) |
  | GET `/api/recipes/{slug}` | RecipeDetail |
  (GĐ4 thêm `sort=rating`.)

- [ ] **Step 1: Viết test fail**

```java
// RecipeQueryServiceIT — @SpringBootTest + Testcontainers; dữ liệu: A{trứng, cà chua}, B{trứng}, C{trứng, cà chua, hành lá} đều PUBLISHED, D{trứng, cà chua} DRAFT
@Test void containsAllIngredients() {
  Page<RecipeCard> p = service.search(new RecipeSearch(null, null, null, List.of(trung, caChua), null), PageRequest.of(0, 12));
  assertThat(p.getContent()).extracting(RecipeCard::slug).containsExactlyInAnyOrder("a", "c");
  assertThat(p.getTotalElements()).isEqualTo(2);
}
@Test void qMatchesWithoutDiacritics() { /* title "Trứng chiên" → q "trung chien" tìm thấy */ }
@Test void draftsNeverListed() {}
@Test void draftSlugIs404ForAnonymousAndOthers() { /* viewer null → NOT_FOUND; member khác → NOT_FOUND */ }
@Test void draftVisibleToAuthorAndModerator() {}
@Test void newestFirstByPublishedAt() {}

// PublicRecipeControllerTest
@Test void searchIsPublic() { /* GET /api/recipes không đăng nhập → 200 */ }
@Test void sizeCappedAt50() { /* size=500 → Pageable.size 50 */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='RecipeQueryServiceIT,PublicRecipeControllerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.** Giới hạn size: `spring.data.web.pageable.max-page-size: 50`, `default-page-size: 12`; serialize Page dạng DTO (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(recipe): public search including ingredient matching"`

---

### Task 5: Gộp nguyên liệu

**Files:**
- Modify: `catalog/IngredientService.java`, `catalog/IngredientRepository.java`, `catalog/AdminIngredientController.java`
- Test: `catalog/IngredientMergeIT.java`

**Interfaces:**
- Produces: `void merge(Long sourceId, Long targetId)` — cùng id → `VALIDATION_ERROR`; một trong hai không tồn tại → `NOT_FOUND`. Trong một transaction, 3 câu native `@Modifying`:
  ```sql
  DELETE FROM recipe_ingredients s WHERE s.ingredient_id = :src
    AND EXISTS (SELECT 1 FROM recipe_ingredients t WHERE t.recipe_id = s.recipe_id AND t.ingredient_id = :tgt);
  UPDATE recipe_ingredients SET ingredient_id = :tgt WHERE ingredient_id = :src;
  DELETE FROM ingredients WHERE id = :src;
  ```
  Endpoint: POST `/api/admin/ingredients/{id}/merge` `{targetId}` → 204 (CATALOG_MANAGE). GĐ5 thêm 2 câu tương tự cho `bowl_items`.

- [ ] **Step 1: Viết test fail**

```java
@Test void mergeRepointsReferences() { /* recipe R dùng "Ca chua" (src) → merge vào "Cà chua" → R dùng "Cà chua", src bị xóa */ }
@Test void mergeDropsDuplicateWhenRecipeHasBoth() { /* R có cả src và tgt → còn 1 dòng tgt, giữ quantity của tgt */ }
@Test void mergeIntoSelfRejected() {}
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest=IngredientMergeIT` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(catalog): merge duplicate ingredients"`

---

### Task 6: Frontend — trang công khai

**Files:**
- Create: `frontend/src/api/recipes.ts`, `src/utils/units.ts`, `src/components/RecipeCard.vue`, `src/pages/{RecipeListPage,RecipeDetailPage}.vue`
- Modify: `src/pages/HomePage.vue` (8 công thức mới nhất + danh mục), `src/layouts/PublicLayout.vue` (ô tìm kiếm), `src/router/index.ts`
- Test: `src/utils/__tests__/units.spec.ts`, `src/pages/__tests__/RecipeListPage.spec.ts`, `src/pages/__tests__/RecipeDetailPage.spec.ts`

**Interfaces:**
- Produces:
  ```ts
  export type Unit = 'G'|'KG'|'ML'|'L'|'TSP'|'TBSP'|'CUP'|'PIECE'|'PINCH'
  export const UNIT_LABELS: Record<Unit, string>  // g, kg, ml, l, thìa cà phê, thìa canh, cốc, cái/quả, nhúm
  export function formatQuantity(q: number, u: Unit): string   // 200,'G' → "200 g"; 1.5,'TBSP' → "1,5 thìa canh"
  export interface Page<T> { content: T[]; page: { size: number; number: number; totalElements: number; totalPages: number } }
  export interface RecipeSearch { q?: string; category?: string; tag?: string; ingredientIds?: number[]; sort?: 'newest'|'rating'; page?: number }
  export function searchRecipes(s: RecipeSearch): Promise<Page<RecipeCard>>   // ingredientIds → "1,2"
  export function toQuery(s: RecipeSearch): Record<string, string>; export function fromQuery(q: LocationQuery): RecipeSearch
  export function getRecipe(slug: string): Promise<RecipeDetail>
  // RecipeCard, RecipeDetail: interface TS khớp record Java ở Task 2
  ```
- Route: `/recipes` (bộ lọc đồng bộ URL query, chọn nguyên liệu dạng chip có emoji), `/recipes/:slug`.

- [ ] **Step 1: Viết test fail**

```ts
it('formats quantity with Vietnamese labels', () => { expect(formatQuantity(1.5, 'TBSP')).toBe('1,5 thìa canh') })
it('round-trips search to URL query', () => {
  const s = { q: 'trứng', ingredientIds: [1, 2], page: 1 }
  expect(toQuery(s)).toEqual({ q: 'trứng', ingredientIds: '1,2', page: '1' })
  expect(fromQuery(toQuery(s))).toEqual(s)
})
it('list page calls API with ingredientIds from URL', async () => { /* route ?ingredientIds=3,4 → searchRecipes gọi với [3,4] */ })
it('detail shows not-found message on 404', async () => { /* "Không tìm thấy công thức" */ })
it('detail shows rejection reason to author', async () => { /* status REJECTED → hiện lý do */ })
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.** CSS tự viết, responsive (lưới 1 cột trên mobile, 3–4 cột desktop).
- [ ] **Step 4: Chạy pass** — Run: `npm run test && npm run type-check` — Expected: xanh.
- [ ] **Step 5: Commit** — `git commit -am "feat(frontend): public recipe list and detail"`

---

### Task 7: Frontend — trình soạn công thức & trang duyệt

**Files:**
- Create: `frontend/src/components/IngredientPicker.vue`, `src/features/recipe-editor/editorState.ts`, `src/pages/{MyRecipesPage,RecipeEditorPage}.vue`, `src/pages/admin/AdminRecipesPage.vue`
- Modify: `src/router/index.ts` (`/me/recipes`, `/me/recipes/new`, `/me/recipes/:id/edit` — `permission: 'RECIPE_WRITE'`; `/admin/recipes` — `CONTENT_MODERATE`), `src/layouts/AdminLayout.vue`, `src/api/messages.ts`
- Test: `src/features/recipe-editor/__tests__/editorState.spec.ts`, `src/pages/__tests__/RecipeEditorPage.spec.ts`

**Interfaces:**
- Produces (logic thuần, dễ test):
  ```ts
  export interface EditorState { /* các trường RecipeRequest + ingredients: Line[] + steps: { content: string; media: MediaRef|null }[] */ }
  export function addIngredient(s: EditorState, ing: Ingredient): EditorState     // đã có → không đổi, trả nguyên s
  export function moveStep(s: EditorState, from: number, to: number): EditorState
  export function toRequest(s: EditorState): RecipeRequest                         // thứ tự mảng = position/stepNo
  ```
- `IngredientPicker`: autocomplete gọi `listIngredients(q)` (debounce 250ms); không có kết quả → mục "Thêm “{q}” mới" mở dialog chọn nhóm → `suggestIngredient`.
- `RecipeEditorPage`: nút "Lưu nháp" (create/update) và "Gửi duyệt" (lưu rồi submit). Đang sửa công thức PUBLISHED → hiện cảnh báo "Lưu thay đổi sẽ gửi công thức đi duyệt lại". Lỗi `RECIPE_INCOMPLETE` → hiện `detail`.
- `AdminRecipesPage`: tab PENDING/HIDDEN, xem trước, "Duyệt", "Từ chối" (dialog bắt buộc lý do), "Ẩn", "Hiện lại".

- [ ] **Step 1: Viết test fail**

```ts
it('does not add the same ingredient twice', () => { expect(addIngredient(addIngredient(s, egg), egg).ingredients).toHaveLength(1) })
it('moveStep reorders and toRequest keeps order', () => { /* [A,B,C] move(2,0) → toRequest.steps contents [C,A,B] */ })
it('editor warns when editing published recipe', async () => { /* status PUBLISHED → text cảnh báo hiển thị */ })
it('editor shows missing fields from RECIPE_INCOMPLETE', async () => { /* submit rejects ApiError(400,'RECIPE_INCOMPLETE','Còn thiếu: danh mục') → hiện "Còn thiếu: danh mục" */ })
it('reject dialog requires reason', async () => { /* nút xác nhận disabled khi lý do trống */ })
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Cổng giai đoạn** — Run: `cd backend && ./mvnw verify && cd ../frontend && npm run test && npm run type-check` — Expected: tất cả xanh.
- [ ] **Step 5: Kiểm tra tay** — member viết công thức → gửi duyệt → moderator duyệt → khách thấy ở `/recipes`; tìm theo 2 nguyên liệu ra đúng.
- [ ] **Step 6: Commit** — `git commit -am "feat(frontend): recipe editor and moderation queue"`
