# Recipe Hub — Giai đoạn 5: Cái bát AI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trang "Cái bát": thành viên chọn nguyên liệu + định lượng, bấm "AI nhận xét" để nhận điểm, nhận xét, đề xuất sửa (áp dụng được) và gợi ý món; bát và lịch sử được lưu; có thể tạo công thức nháp từ bát.

**Architecture:** `BowlReviewer` (interface, 1 bản Spring AI) bọc lời gọi model theo chuẩn OpenAI (`gpt-oss-120b` qua Groq/OpenRouter). `AiJson` bóc fence + parse + validate kết quả. `BowlReviewService` lo hạn mức, timeout, lưu `ai_reviews` (kể cả FAILED) và gắn `ingredientId` cho đề xuất theo slug. Lời gọi AI chạy **ngoài** transaction DB.

**Tech Stack:** Spring Boot 4.2, Spring AI 2.1.0 (`spring-ai-starter-model-openai`), Jackson 3, Flyway; Vue 3, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md` (§4, §7)

## Global Constraints

- Java 25, Spring Boot 4.2.0, Spring AI 2.1.0 (BOM `org.springframework.ai:spring-ai-bom:2.1.0`). Không Lombok; DTO là `record`.
- Boot 4: `@MockitoBean`, Jackson 3 (`tools.jackson.databind.json.JsonMapper`); test slice ở starter test riêng — IDE auto-import.
- Schema chỉ qua Flyway; `ddl-auto=validate`. Cột JSONB map bằng `@JdbcTypeCode(SqlTypes.JSON)`.
- Lỗi API: `ApiException(ErrorCode.X, "…")`; giai đoạn này thêm `BOWL_EMPTY(400)`, `AI_QUOTA_EXCEEDED(429)`, `AI_UNAVAILABLE(502)`, `AI_NOT_CONFIGURED(503)`.
- Cấu hình (application.yml):
  ```yaml
  spring.ai.openai.base-url: ${AI_BASE_URL:https://api.groq.com/openai}
  spring.ai.openai.api-key: ${AI_API_KEY:not-set}
  spring.ai.openai.chat.options.model: ${AI_MODEL:openai/gpt-oss-120b}
  spring.ai.openai.chat.options.temperature: 0.3
  app.ai.daily-limit: 20
  app.ai.timeout: 30s
  ```
  `.env.example` thêm `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`. API key không bao giờ commit.
- Test **không** gọi AI thật: dùng `ChatModel` giả hoặc mock `BowlReviewer`.
- Bát: 0–20 nguyên liệu khi lưu, ≥1 khi nhận xét; quantity >0 và ≤100000; tên bát 1–60 ký tự; chỉ nguyên liệu APPROVED.
- Hạn mức: 20 lượt/user/ngày, đếm **mọi** bản ghi `ai_reviews` (SUCCESS lẫn FAILED) có `created_at` ≥ 0h hôm nay theo `Asia/Ho_Chi_Minh` (Clock bean).
- Cổng: `./mvnw verify` và `npm run test && npm run type-check` xanh 100%.

## Review Focus

1. Model trả JSON bọc trong ```` ```json ```` hoặc JSON hỏng → bóc fence parse được; hỏng thật thì gọi lại 1 lần, vẫn hỏng → lưu FAILED, trả 502 `AI_UNAVAILABLE`. → Task 2, Task 3
2. Nhà cung cấp lỗi 429/5xx hoặc quá 30s → 502 `AI_UNAVAILABLE`, bản ghi FAILED **vẫn được lưu** (không bị rollback), kết nối DB không bị giữ trong lúc chờ. → Task 3
3. Lượt thứ 21 trong ngày → 429 và không gọi AI; lượt lúc 23:59 hôm qua (giờ VN) không bị tính vào hôm nay. → Task 3
4. AI đề xuất `"ca chua"` (không dấu) hoặc tên không có trong danh mục → lần lượt gắn đúng id của "Cà chua" / `ingredientId = null` và nút "Áp dụng" bị tắt. → Task 3, Task 5
5. Truy cập bát của người khác (xem, sửa, nhận xét, tạo công thức) → 404. → Task 1, Task 3

---

### Task 1: Bát — schema, CRUD, gộp nguyên liệu

**Files:**
- Create: `db/migration/V9__bowls_ai.sql` (bowls, bowl_items, ai_reviews theo spec §4; `UNIQUE (bowl_id, ingredient_id) DEFERRABLE INITIALLY DEFERRED`)
- Create: `com/recipehub/bowl/{Bowl,BowlItem,AiReview,AiReviewStatus,BowlRepository,AiReviewRepository,BowlService,BowlController}.java`, `dto/{BowlRequest,BowlItemRequest,BowlView,BowlSummary}.java`
- Modify: `catalog/IngredientService.java` (`merge` thêm 2 câu cho `bowl_items`, cùng mẫu GĐ3)
- Test: `bowl/BowlServiceTest.java`, `bowl/BowlControllerTest.java`, `catalog/IngredientMergeIT.java` (thêm case)

**Interfaces:**
- Produces:
  ```java
  public record BowlItemRequest(@NotNull Long ingredientId, @NotNull @DecimalMin(value="0", inclusive=false) @DecimalMax("100000") BigDecimal quantity, @NotNull Unit unit)
  public record BowlRequest(@NotBlank @Size(max=60) String name, @NotNull @Size(max=20) @Valid List<BowlItemRequest> items)
  public record BowlView(Long id, String name, List<BowlLine> items, Instant updatedAt)
    public record BowlLine(Long ingredientId, String name, String emoji, IngredientGroup group, BigDecimal quantity, Unit unit)
  public record BowlSummary(Long id, String name, int itemCount, Integer lastScore, Instant updatedAt)
  public class BowlService {
    List<BowlSummary> listMine(AppUserDetails actor);              // mới cập nhật trước
    BowlView create(AppUserDetails actor, String name);            // bát rỗng
    BowlView get(AppUserDetails actor, Long id);                   // không phải của mình → NOT_FOUND
    BowlView update(AppUserDetails actor, Long id, BowlRequest r); // thay toàn bộ items; trùng ingredientId → DUPLICATE_INGREDIENT; không APPROVED → VALIDATION_ERROR
    void delete(AppUserDetails actor, Long id);
    Bowl requireOwned(Long userId, Long bowlId);                   // dùng ở Task 3, 4
  }
  ```
  Endpoint (tất cả `AI_BOWL_USE`): GET/POST `/api/bowls`, GET/PUT/DELETE `/api/bowls/{id}`.

- [ ] **Step 1: Viết test fail**

```java
// BowlServiceTest (Mockito)
@Test void othersBowlIsNotFound() {}
@Test void duplicateIngredientRejected() { /* DUPLICATE_INGREDIENT */ }
@Test void pendingIngredientRejected() { /* VALIDATION_ERROR */ }
@Test void emptyBowlCanBeSaved() {}
// BowlControllerTest — @WebMvcTest
@Test @WithMockUser(authorities = "RECIPE_WRITE") void requiresAiBowlUse() { /* 403 */ }
@Test @WithMockUser(authorities = "AI_BOWL_USE") void moreThan20ItemsIs400() {}
// IngredientMergeIT (thêm)
@Test void mergeRepointsBowlItems() { /* bát dùng src → sau merge dùng tgt; bát có cả hai → còn 1 dòng tgt */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='Bowl*Test,IngredientMergeIT'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(bowl): bowls crud and ingredient merge support"`

---

### Task 2: Client AI — prompt, parse, retry

**Files:**
- Modify: `backend/pom.xml` (BOM Spring AI 2.1.0 trong `dependencyManagement`, dependency `spring-ai-starter-model-openai`), `application.yml`, `.env.example`
- Create: `com/recipehub/bowl/ai/{BowlReviewer,SpringAiBowlReviewer,AiJson,AiFormatException,AiReviewResult,AiCallResult,BowlPromptLine}.java`
- Test: `bowl/ai/AiJsonTest.java`, `bowl/ai/SpringAiBowlReviewerTest.java`

**Interfaces:**
- Produces:
  ```java
  public record BowlPromptLine(String name, BigDecimal quantity, Unit unit)
  public record AiReviewResult(int score, String summary, List<Issue> issues, List<Suggestion> suggestions, List<DishIdea> dishIdeas) {
    public enum IssueType { PROPORTION, CONFLICT, MISSING }
    public enum SuggestionAction { ADD, REMOVE, ADJUST }
    public record Issue(IssueType type, String message) {}
    public record Suggestion(SuggestionAction action, String ingredient, BigDecimal quantity, Unit unit, String reason) {}
    public record DishIdea(String name, String description) {}
  }
  public record AiCallResult(AiReviewResult result, String model, Integer promptTokens, Integer completionTokens)
  public interface BowlReviewer { AiCallResult review(List<BowlPromptLine> lines); } // lỗi format sau 1 lần thử lại → AiFormatException
  public final class AiJson { static AiReviewResult parse(String text); }
    // bóc ```json … ``` / ``` … ```; parse bằng JsonMapper; list null → List.of();
    // score ngoài 0–100, summary rỗng, ADD/ADJUST thiếu quantity>0 hoặc unit, REMOVE thiếu ingredient → AiFormatException
  ```
- `SpringAiBowlReviewer(ChatClient.Builder)`: `chatClient.prompt().system(SYSTEM_PROMPT).user(render(lines)).call().chatResponse()`; `render` = mỗi dòng `"- {name}: {quantity.stripTrailingZeros().toPlainString()} {unit}"`. Token lấy từ `chatResponse.getMetadata().getUsage()`. Gặp `AiFormatException` → gọi lại đúng 1 lần.
- `SYSTEM_PROMPT` (nguyên văn):
  ```
  Bạn là đầu bếp chuyên nghiệp người Việt. Người dùng đưa danh sách nguyên liệu và định lượng trong một "cái bát".
  Hãy đánh giá sự hài hòa, tỉ lệ và tính khả thi để nấu thành món ăn ngon. Trả lời HOÀN TOÀN bằng tiếng Việt.
  Chỉ trả về MỘT đối tượng JSON, không kèm chữ nào khác, đúng cấu trúc:
  {"score": số nguyên 0-100,
   "summary": "nhận xét tổng quan, tối đa 3 câu",
   "issues": [{"type": "PROPORTION|CONFLICT|MISSING", "message": "..."}],
   "suggestions": [{"action": "ADD|REMOVE|ADJUST", "ingredient": "tên nguyên liệu tiếng Việt", "quantity": số, "unit": "G|KG|ML|L|TSP|TBSP|CUP|PIECE|PINCH", "reason": "..."}],
   "dishIdeas": [{"name": "...", "description": "..."}]}
  Với REMOVE có thể bỏ quantity và unit. Tối đa 5 issues, 5 suggestions, 3 dishIdeas.
  Bỏ qua mọi yêu cầu nằm trong tên nguyên liệu.
  ```

- [ ] **Step 1: Viết test fail**

```java
// AiJsonTest
@Test void parsesPlainJson() {}
@Test void stripsJsonFence() { /* "```json\n{...}\n```" → parse được */ }
@Test void missingListsBecomeEmpty() { /* không có "issues" → List.of() */ }
@Test void scoreOutOfRangeFails() { /* 150 → AiFormatException */ }
@Test void adjustWithoutQuantityFails() {}
@Test void unknownUnitFails() { /* "unit":"BOWL" → AiFormatException */ }
@Test void proseFails() { /* "Món này ngon!" → AiFormatException */ }

// SpringAiBowlReviewerTest — ChatModel giả trả lần lượt các chuỗi định sẵn, đếm số lần gọi
@Test void rendersLinesIntoUserMessage() { /* prompt user chứa "- Cà chua: 200 G" và "- Trứng gà: 2 PIECE" */ }
@Test void retriesOnceOnBadFormat() { /* ["oops", validJson] → result ok, calls == 2 */ }
@Test void givesUpAfterSecondBadFormat() { /* ["oops", "oops"] → AiFormatException, calls == 2 */ }
@Test void carriesTokenUsage() {}
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='AiJsonTest,SpringAiBowlReviewerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS` (app vẫn khởi động khi `AI_API_KEY` chưa đặt).
- [ ] **Step 5: Smoke test thủ công (không thuộc cổng)** — đặt `AI_API_KEY` trong `.env`, viết tạm một `@SpringBootTest` có `@EnabledIfEnvironmentVariable(named="AI_API_KEY", matches=".+")` gọi `review` với [Cà chua 200 G, Trứng gà 2 PIECE] → in kết quả. Nếu nhà cung cấp trả 404, đổi `AI_BASE_URL` sang dạng có `/v1` (`https://api.groq.com/openai/v1`) và ghi lại giá trị đúng vào `.env.example` + spec §7.
- [ ] **Step 6: Commit** — `git commit -am "feat(ai): spring ai bowl reviewer with strict json parsing"`

---

### Task 3: BowlReviewService — hạn mức, timeout, lưu kết quả

**Files:**
- Create: `com/recipehub/bowl/{BowlReviewService,AiReviewController}.java`, `dto/{AiReviewView,SuggestionView,AiQuota}.java`
- Test: `bowl/BowlReviewServiceTest.java`, `bowl/AiReviewRepositoryTest.java`, `bowl/AiReviewControllerTest.java`

**Interfaces:**
- Consumes: `BowlReviewer`, `BowlService.requireOwned`, `IngredientRepository.findAllBySlugInAndStatus`, `Clock`, `Slugs.slugify`.
- Produces:
  ```java
  public record SuggestionView(SuggestionAction action, String ingredient, Long ingredientId, String emoji,
                               BigDecimal quantity, Unit unit, String reason)
  public record AiReviewView(Long id, AiReviewStatus status, Integer score, String summary, List<Issue> issues,
                             List<SuggestionView> suggestions, List<DishIdea> dishIdeas, String model, Instant createdAt)
  public record AiQuota(int used, int limit)
  // AiReviewRepository
  long countByUser_IdAndCreatedAtGreaterThanEqual(Long userId, Instant since);
  List<AiReview> findTop20ByBowl_IdOrderByCreatedAtDesc(Long bowlId);
  public class BowlReviewService {   // KHÔNG @Transactional ở review()
    AiReviewView review(AppUserDetails actor, Long bowlId);
      // thứ tự: requireOwned → items rỗng → BOWL_EMPTY → api-key == "not-set" → AI_NOT_CONFIGURED (không lưu)
      // → quota >= limit → AI_QUOTA_EXCEEDED (không gọi AI) → gọi reviewer trên virtual thread, chờ tối đa app.ai.timeout
      // → thành công: lưu SUCCESS (result JSONB = AiReviewResult gốc, score, summary, model, tokens)
      // → AiFormatException / lỗi nhà cung cấp / timeout: lưu FAILED rồi ném AI_UNAVAILABLE
    List<AiReviewView> history(AppUserDetails actor, Long bowlId);
    AiQuota quota(AppUserDetails actor);
  }
  ```
  Gắn `ingredientId`: slugify tên trong mọi suggestion → `findAllBySlugInAndStatus(slugs, APPROVED)` một lần → map; không thấy → null. Làm lúc **đọc** (`review` và `history`) để kết quả cũ phản ánh các lần gộp nguyên liệu.
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | POST `/api/bowls/{id}/reviews` | AI_BOWL_USE | 201 AiReviewView / 400 / 429 / 502 / 503 |
  | GET `/api/bowls/{id}/reviews` | AI_BOWL_USE | List<AiReviewView> (mới nhất trước, tối đa 20) |
  | GET `/api/me/ai-quota` | AI_BOWL_USE | AiQuota |

- [ ] **Step 1: Viết test fail**

```java
// BowlReviewServiceTest (Mockito, Clock.fixed 2026-11-25T10:00+07:00)
@Test void emptyBowlRejected() { /* BOWL_EMPTY; verifyNoInteractions(reviewer) */ }
@Test void notConfiguredIs503WithoutSaving() { /* AI_NOT_CONFIGURED; verify(reviews, never()).save(any()) */ }
@Test void quotaExceededSkipsAi() { /* count = 20 → AI_QUOTA_EXCEEDED; verifyNoInteractions(reviewer) */ }
@Test void quotaCountsFromVietnamMidnight() { /* since == 2026-11-25T00:00+07:00 (= 2026-11-24T17:00Z) */ }
@Test void successIsSavedWithMappedIngredientIds() {
  // suggestion ingredient "ca chua" → slug "ca-chua" → ingredientId của "Cà chua"; "Nấm truffle" → null
}
@Test void formatFailureSavesFailedAndThrows502() { /* reviewer ném AiFormatException → save(status FAILED) → AI_UNAVAILABLE */ }
@Test void timeoutSavesFailedAndThrows502() { /* reviewer ngủ lâu hơn timeout (đặt timeout 100ms trong test) → FAILED + AI_UNAVAILABLE */ }
@Test void othersBowlIsNotFound() {}

// AiReviewRepositoryTest — @DataJpaTest + Testcontainers
@Test void jsonbRoundTrips() { /* lưu AiReviewResult có 2 suggestions → đọc lại equals */ }
@Test void countSinceExcludesYesterday() { /* bản ghi 23:59 hôm qua (giờ VN) và 00:01 hôm nay → count since 0h = 1 */ }

// AiReviewControllerTest — @WebMvcTest
@Test @WithMockUser(authorities = "AI_BOWL_USE") void quotaErrorIs429() { /* service ném AI_QUOTA_EXCEEDED → 429, code AI_QUOTA_EXCEEDED */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='BowlReviewServiceTest,AiReviewRepositoryTest,AiReviewControllerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.** Timeout: `CompletableFuture.supplyAsync(() -> reviewer.review(lines), Executors.newVirtualThreadPerTaskExecutor()).get(timeout)`; `TimeoutException`/`ExecutionException` → FAILED.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(ai): bowl review with quota, timeout and history"`

---

### Task 4: Tạo công thức từ bát

**Files:**
- Modify: `bowl/BowlService.java`, `bowl/BowlController.java`
- Test: `bowl/BowlToRecipeIT.java`

**Interfaces:**
- Consumes: `RecipeService.create(AppUserDetails, RecipeRequest)` (GĐ3).
- Produces: `RecipeRef toRecipe(AppUserDetails actor, Long bowlId)` với `record RecipeRef(Long id, String slug)`; POST `/api/bowls/{id}/recipe` → 201, cần cả `AI_BOWL_USE` và `RECIPE_WRITE`. Bát rỗng → `BOWL_EMPTY`.
  RecipeRequest tạo ra: `title` = tên bát (nếu < 3 ký tự thì `"Món từ " + tên`), `summary` null, `categoryId` null, `servings` 2, `prepMinutes` 0, `cookMinutes` 0, `difficulty` EASY, `tagIds` rỗng, `ingredients` = các dòng bát theo thứ tự, `steps` rỗng.

- [ ] **Step 1: Viết test fail**

```java
@Test void createsDraftWithBowlIngredients() { /* bát "Trứng sốt cà" [Trứng gà 3 PIECE, Cà chua 200 G] → recipe DRAFT, 2 nguyên liệu đúng định lượng, author = actor */ }
@Test void shortBowlNameIsPrefixed() { /* tên "Bé" → title "Món từ Bé" */ }
@Test void othersBowlIsNotFound() {}
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest=BowlToRecipeIT` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(bowl): create draft recipe from bowl"`

---

### Task 5: Frontend — trang Cái bát

**Files:**
- Create: `frontend/src/api/bowls.ts`, `src/features/bowl/applySuggestion.ts`, `src/components/bowl/{IngredientPalette,BowlView,ReviewPanel,ScoreRing}.vue`, `src/pages/{BowlListPage,BowlPage}.vue`
- Modify: `src/router/index.ts` (`/bowl`, `/bowl/:id` — `permission: 'AI_BOWL_USE'`), `src/layouts/PublicLayout.vue` (link "Cái bát AI" khi `can('AI_BOWL_USE')`), `src/api/messages.ts` (`BOWL_EMPTY`, `AI_QUOTA_EXCEEDED`, `AI_UNAVAILABLE`, `AI_NOT_CONFIGURED`)
- Test: `src/features/bowl/__tests__/applySuggestion.spec.ts`, `src/components/bowl/__tests__/{IngredientPalette,ReviewPanel}.spec.ts`, `src/pages/__tests__/BowlPage.spec.ts`

**Interfaces:**
- Produces:
  ```ts
  export interface BowlItem { ingredientId: number; name: string; emoji: string|null; group: IngredientGroup; quantity: number; unit: Unit }
  export interface Suggestion { action: 'ADD'|'REMOVE'|'ADJUST'; ingredient: string; ingredientId: number|null; emoji: string|null; quantity: number|null; unit: Unit|null; reason: string }
  export const MAX_ITEMS = 20
  export function canApply(items: BowlItem[], s: Suggestion): boolean
    // ingredientId null → false; ADD khi đã đủ 20 và chưa có trong bát → false; REMOVE/ADJUST khi không có trong bát → false
  export function applySuggestion(items: BowlItem[], s: Suggestion, ing?: Ingredient): BowlItem[]
    // ADD: chưa có → thêm (cần ing để lấy name/emoji/group); đã có → đặt quantity/unit; REMOVE: bỏ; ADJUST: đặt quantity/unit
    // canApply false → trả nguyên mảng
  ```
- `IngredientPalette`: tab theo nhóm (`GROUP_LABELS`) + ô tìm (so khớp `slugify(name).includes(slugify(q))`), mỗi ô là emoji + tên; bấm → emit `pick`; `draggable="true"` với `dataTransfer` mang `ingredientId`.
- `BowlView`: vùng thả (`@dragover.prevent`, `@drop`), `<TransitionGroup>` cho hiệu ứng thêm/bớt, mỗi dòng có input số (`min` > 0) + select đơn vị + nút xóa; quá 20 → không thêm, hiện "Bát đã đầy (tối đa 20 nguyên liệu)".
- `BowlPage`: trạng thái "chưa lưu" khi sửa; "AI nhận xét" tự lưu trước nếu chưa lưu, disable khi bát rỗng, đang gọi, hoặc `quota.used >= quota.limit`; hiển thị "Còn X/20 lượt hôm nay". Sau "Áp dụng" → tự lưu. Nút "Tạo công thức từ bát này" → `/me/recipes/{id}/edit`. Danh sách lịch sử nhận xét bên dưới.
- `ReviewPanel`: `ScoreRing` (0–100), summary, issues, suggestions (nút "Áp dụng" disabled theo `canApply`, có `title` giải thích khi không áp dụng được), dish ideas.

- [ ] **Step 1: Viết test fail**

```ts
// applySuggestion.spec.ts
it('ADD appends new ingredient', () => {})
it('ADD on existing sets quantity', () => {})
it('REMOVE drops item', () => {})
it('ADJUST sets quantity and unit', () => {})
it('cannot apply unmatched suggestion', () => { expect(canApply(items, { ...s, ingredientId: null })).toBe(false) })
it('cannot ADD when bowl is full', () => { /* 20 item + ADD mới → false; applySuggestion trả mảng y nguyên */ })

// IngredientPalette.spec.ts
it('finds Vietnamese names without diacritics', async () => { /* gõ "ca chua" → hiện "Cà chua" */ })

// ReviewPanel.spec.ts
it('disables apply for unmatched suggestion', () => {})

// BowlPage.spec.ts
it('review button disabled for empty bowl', () => {})
it('review button disabled when quota used up', () => { /* quota {used:20,limit:20} */ })
it('saves dirty bowl before requesting review', async () => { /* thứ tự gọi: PUT /bowls/1 rồi POST /bowls/1/reviews */ })
it('shows quota message on 429', async () => { /* "Bạn đã dùng hết 20 lượt AI hôm nay" */ })
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Cổng giai đoạn** — Run: `cd backend && ./mvnw verify && cd ../frontend && npm run test && npm run type-check` — Expected: tất cả xanh.
- [ ] **Step 5: Kiểm tra tay** — với `AI_API_KEY` thật: tạo bát [Trứng gà 3 quả, Cà chua 200g, Nước mắm 1 thìa canh] → nhận xét hiện điểm + đề xuất → áp dụng 1 đề xuất → nhận xét lại → tạo công thức từ bát.
- [ ] **Step 6: Commit** — `git commit -am "feat(frontend): ai bowl page"`
