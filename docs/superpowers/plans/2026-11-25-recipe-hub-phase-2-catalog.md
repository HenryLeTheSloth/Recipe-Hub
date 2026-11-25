# Recipe Hub — Giai đoạn 2: Ảnh & danh mục — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upload ảnh an toàn (lưu local), avatar, danh mục món, tag và danh mục nguyên liệu (có seed, thành viên đề xuất nguyên liệu mới, moderator duyệt).

**Architecture:** `StorageService` (interface, 1 bản local) giữ file; bảng `media` giữ metadata. `Slugs` bỏ dấu tiếng Việt dùng chung cho mọi slug và cho tìm kiếm không dấu. Catalog đọc công khai, ghi cần `CATALOG_MANAGE`.

**Tech Stack:** Spring Boot 4.2, Data JPA, Flyway; Vue 3, PrimeVue, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-25-recipe-hub-design.md` (§4, §8, §9)

## Global Constraints

- Java 25, Spring Boot 4.2.0, MapStruct 1.6.x. Không Lombok; DTO là `record`.
- Boot 4: `@MockitoBean`, Jackson 3; test slice ở starter test riêng — IDE auto-import.
- Package `com.recipehub.<feature>`. Schema chỉ qua Flyway; `ddl-auto=validate`.
- Lỗi API: `ApiException(ErrorCode.X, "…")`; giai đoạn này thêm `MEDIA_TYPE(415)`, `MEDIA_TOO_LARGE(413)`, `NAME_TAKEN(409)`, `IN_USE(409)`.
- Ảnh ≤ 5MB, chỉ JPEG/PNG/WEBP theo magic bytes; `spring.servlet.multipart.max-file-size=5MB`, `max-request-size=6MB`.
- Cổng: `./mvnw verify` và `npm run test && npm run type-check` xanh 100%.

## Review Focus

1. File `.jpg` thực chất là HTML/SVG → 415 `MEDIA_TYPE`, không lưu file. → Task 1
2. Upload 6MB → 413 `MEDIA_TOO_LARGE` dạng ProblemDetail JSON (không phải trang lỗi mặc định của Tomcat). → Task 1
3. Tên tiếng Việt cho slug: `"Đậu phụ"` → `"dau-phu"`; `"Cà chua"` và `"Ca chua"` khác tên nhưng cùng slug → bản thứ hai thành `"ca-chua-2"`. → Task 2, Task 3
4. Thành viên đề xuất `"cà chua "` khi đã có `"Cà chua"` → trả nguyên liệu sẵn có (200), không tạo bản PENDING trùng; tìm `"ca chua"` (không dấu) vẫn ra `"Cà chua"`. → Task 3
5. Đặt avatar bằng `mediaId` của người khác → 403. → Task 1

---

### Task 1: Media — upload, phục vụ file, avatar

**Files:**
- Create: `db/migration/V4__media.sql` (bảng `media`; `ALTER TABLE users ADD avatar_media_id … ON DELETE SET NULL`)
- Create: `com/recipehub/media/{Media,MediaRepository,StorageService,LocalStorageService,MediaTypes,MediaService,MediaController}.java`, `dto/MediaResponse.java`
- Modify: `user/User.java` (`@ManyToOne Media avatar`), `user/MeController.java` (+`PUT /api/me/avatar`), `MeResponse` (avatarUrl), `common/GlobalExceptionHandler.java` (+`MaxUploadSizeExceededException` → 413 `MEDIA_TOO_LARGE`), `config/SecurityConfig.java` (permitAll `GET /api/media/*/file`)
- Test: `media/MediaTypesTest.java`, `media/LocalStorageServiceTest.java`, `media/MediaServiceTest.java`, `media/MediaControllerTest.java`

**Interfaces:**
- Produces:
  ```java
  public interface StorageService { String store(InputStream in, String extension); Resource load(String key); void delete(String key); }
    // LocalStorageService: thư mục app.storage.dir; key = UUID + "." + ext; key chứa '/', '\' hoặc ".." → NOT_FOUND
  public final class MediaTypes { static Optional<String> detect(byte[] head); } // "image/jpeg" | "image/png" | "image/webp"
    // JPEG FF D8 FF; PNG 89 50 4E 47 0D 0A 1A 0A; WEBP "RIFF" ???? "WEBP"
  public record MediaResponse(Long id, String url)          // url = "/api/media/{id}/file"
  public class MediaService {
    MediaResponse upload(Long ownerId, MultipartFile file);  // rỗng/sai loại → MEDIA_TYPE; > 5MB → MEDIA_TOO_LARGE
    Media requireOwned(Long mediaId, Long userId);           // không tồn tại → NOT_FOUND; của người khác → FORBIDDEN
    static String url(Media m);                              // null-safe
  }
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | POST `/api/media` (multipart `file`) | MEDIA_UPLOAD | 201 MediaResponse |
  | GET `/api/media/{id}/file` | công khai | file, đúng `Content-Type`, `Cache-Control: max-age=86400` |
  | PUT `/api/me/avatar` `{mediaId}` | đăng nhập | 200 MeResponse |

- [ ] **Step 1: Viết test fail**

```java
// MediaTypesTest
@Test void detectsPng() { assertThat(MediaTypes.detect(hex("89504E470D0A1A0A0000"))).contains("image/png"); }
@Test void detectsJpeg() {} @Test void detectsWebp() { /* "RIFF" + 4 byte + "WEBP" */ }
@Test void rejectsHtmlAndSvg() { assertThat(MediaTypes.detect("<html>".getBytes())).isEmpty(); assertThat(MediaTypes.detect("<svg ".getBytes())).isEmpty(); }
@Test void rejectsShortInput() { assertThat(MediaTypes.detect(new byte[2])).isEmpty(); }

// LocalStorageServiceTest — @TempDir
@Test void storeThenLoadRoundTrips() {}
@Test void rejectsPathTraversal() { assertThatThrownBy(() -> storage.load("../secret")) /* NOT_FOUND */; }

// MediaServiceTest (Mockito)
@Test void pngNamedJpgIsStoredAsPng() { /* filename "a.jpg", bytes PNG → contentType image/png, ext "png" */ }
@Test void htmlNamedJpgIsRejected() { /* MEDIA_TYPE; verifyNoInteractions(storage) */ }
@Test void overFiveMegabytesRejected() { /* getSize = 5*1024*1024+1 → MEDIA_TOO_LARGE */ }
@Test void requireOwnedRejectsOthersMedia() { /* FORBIDDEN */ }

// MediaControllerTest — @WebMvcTest(MediaController) @Import(SecurityConfig)
@Test void uploadRequiresAuth() { /* 401 */ }
@Test @WithMockUser(authorities = "RECIPE_WRITE") void uploadRequiresMediaUpload() { /* 403 */ }
@Test void maxUploadSizeMapsTo413Json() { /* handler unit: MaxUploadSizeExceededException → 413, code MEDIA_TOO_LARGE */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='Media*Test,LocalStorageServiceTest'` — Expected: lỗi biên dịch.
- [ ] **Step 3: Implement.** `upload` đọc 12 byte đầu để detect, rồi stream file vào storage.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Kiểm tra tay** — `curl -F file=@big.jpg` (6MB) với session member → JSON 413 `MEDIA_TOO_LARGE`.
- [ ] **Step 6: Commit** — `git commit -am "feat(media): safe local image upload and avatar"`

---

### Task 2: Slugs + Category + Tag

**Files:**
- Create: `common/Slugs.java`
- Create: `db/migration/V5__catalog.sql` (categories, tags, ingredients — theo spec §4; `CREATE UNIQUE INDEX ux_ingredients_name ON ingredients (lower(name))`)
- Create: `com/recipehub/catalog/{Category,Tag,CategoryRepository,TagRepository,CategoryService,TagService,CatalogController,AdminCatalogController}.java`, `dto/{NamedRequest,CategoryRequest,CategoryResponse,TagResponse}.java`
- Modify: `config/SecurityConfig.java` (permitAll `GET /api/categories`, `GET /api/tags`, `GET /api/ingredients`)
- Test: `common/SlugsTest.java`, `catalog/CategoryServiceTest.java`, `catalog/CatalogControllerTest.java`

**Interfaces:**
- Produces:
  ```java
  public final class Slugs {
    static String slugify(String text);                           // NFD bỏ dấu, đ/Đ→d, lowercase, ký tự khác [a-z0-9] → '-', gộp & cắt '-'
    static String unique(String base, Predicate<String> exists);  // base, base-2, base-3…
  }
  public record CategoryRequest(@NotBlank @Size(max=50) String name, int sortOrder)
  public record CategoryResponse(Long id, String name, String slug, int sortOrder)
  public record TagResponse(Long id, String name, String slug)    // tạo/sửa tag dùng NamedRequest(@NotBlank @Size(max=50) name)
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | GET `/api/categories` | công khai | List (sắp theo sortOrder, name) |
  | GET `/api/tags` | công khai | List (theo name) |
  | POST/PUT/DELETE `/api/admin/categories[/{id}]` | CATALOG_MANAGE | 201/200/204 |
  | POST/PUT/DELETE `/api/admin/tags[/{id}]` | CATALOG_MANAGE | 201/200/204 |
- Luật: tên trùng (không phân biệt hoa thường) → `NAME_TAKEN`; slug rỗng sau slugify → `VALIDATION_ERROR` "Tên phải chứa chữ hoặc số"; đổi tên → tạo lại slug (unique); xóa bị FK chặn → `IN_USE` (test thật ở GĐ3).

- [ ] **Step 1: Viết test fail**

```java
// SlugsTest
@ParameterizedTest @CsvSource({
  "Cà chua, ca-chua", "Đậu phụ chiên, dau-phu-chien", "'  Phở   bò!! ', pho-bo",
  "Bánh mì & pate, banh-mi-pate", "Gà ĐÔNG TẢO, ga-dong-tao", "Nước mắm Phú Quốc, nuoc-mam-phu-quoc" })
void slugify(String in, String out) { assertThat(Slugs.slugify(in)).isEqualTo(out); }
@Test void symbolsOnlyGiveEmpty() { assertThat(Slugs.slugify("!!!")).isEmpty(); }
@Test void uniqueAppendsCounter() {
  assertThat(Slugs.unique("ca-chua", Set.of("ca-chua", "ca-chua-2")::contains)).isEqualTo("ca-chua-3");
}

// CategoryServiceTest (Mockito)
@Test void duplicateNameIgnoringCaseIsTaken() { /* existsByNameIgnoreCase → NAME_TAKEN */ }
@Test void renameRegeneratesSlug() {}
@Test void symbolOnlyNameRejected() { /* VALIDATION_ERROR */ }

// CatalogControllerTest — @WebMvcTest({CatalogController, AdminCatalogController}) @Import(SecurityConfig)
@Test void categoriesArePublic() { /* GET /api/categories 200 không đăng nhập */ }
@Test @WithMockUser(authorities = "RECIPE_WRITE") void memberCannotCreateCategory() { /* 403 */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='SlugsTest,CategoryServiceTest,CatalogControllerTest'` — Expected: FAIL.
- [ ] **Step 3: Implement.** `TagService` giống `CategoryService` nhưng không có `sortOrder`.
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(catalog): slugs, categories and tags"`

---

### Task 3: Nguyên liệu — seed, tìm không dấu, đề xuất, duyệt

**Files:**
- Create: `db/migration/V6__seed_catalog.sql`
- Create: `com/recipehub/catalog/{Ingredient,IngredientGroup,IngredientStatus,IngredientRepository,IngredientService,IngredientController,AdminIngredientController}.java`, `dto/{IngredientRequest,IngredientResponse,SuggestResult}.java`
- Test: `catalog/IngredientRepositoryTest.java`, `catalog/IngredientServiceTest.java`, `catalog/IngredientControllerTest.java`

**Interfaces:**
- Seed: 7 category (Món chính, Món canh, Món chay, Tráng miệng, Đồ uống, Ăn vặt, Món nướng), 6 tag (Nhanh gọn, Ít calo, Cho trẻ em, Món Bắc, Món Trung, Món Nam), ~60 nguyên liệu Việt phổ biến `APPROVED` phủ đủ 9 nhóm, mỗi cái có emoji; bắt buộc có `Trứng gà 🥚 EGG_DAIRY`, `Cà chua 🍅 VEGETABLE`, `Hành lá 🌿 VEGETABLE`, `Nước mắm 🫙 SAUCE`, `Thịt ba chỉ 🥓 MEAT`.
- Produces:
  ```java
  public record IngredientRequest(@NotBlank @Size(max=60) String name, @NotNull IngredientGroup group, @Size(max=16) String emoji)
  public record IngredientResponse(Long id, String name, String slug, IngredientGroup group, String emoji, IngredientStatus status)
  public record SuggestResult(IngredientResponse ingredient, boolean created)
  // IngredientRepository
  @Query /* status = :status AND (lower(name) LIKE %lower(q)% OR slug LIKE %qSlug%) AND (group null OR group = :group)
            service đổi q null thành "" (như UserRepository.search); group là enum nên so null an toàn */
  List<Ingredient> search(String q, String qSlug, IngredientGroup group, IngredientStatus status, Limit limit);
  Optional<Ingredient> findByNameIgnoreCase(String name); Optional<Ingredient> findBySlug(String slug);
  List<Ingredient> findAllBySlugInAndStatus(Collection<String> slugs, IngredientStatus status); // GĐ5 dùng
  // IngredientService
  List<IngredientResponse> listApproved(String q, IngredientGroup group);   // tối đa 200
  SuggestResult suggest(Long userId, IngredientRequest r);                  // trùng tên (trim, ignore case) hoặc trùng slug → trả cái có sẵn
  Page<IngredientResponse> adminList(IngredientStatus status, String q, Pageable p);
  IngredientResponse update(Long id, IngredientRequest r, IngredientStatus status);
  void delete(Long id);                                                     // FK chặn → IN_USE
  ```
  | Method & path | Quyền | Kết quả |
  |---|---|---|
  | GET `/api/ingredients?q&group` | công khai | List<IngredientResponse> (chỉ APPROVED) |
  | POST `/api/ingredients` | RECIPE_WRITE | 201 (mới, PENDING) / 200 (có sẵn) → IngredientResponse |
  | GET `/api/admin/ingredients?status&q&page&size` | CATALOG_MANAGE | Page |
  | PUT `/api/admin/ingredients/{id}` `{name, group, emoji, status}` | CATALOG_MANAGE | 200 |
  | DELETE `/api/admin/ingredients/{id}` | CATALOG_MANAGE | 204 / 409 IN_USE |
  (Gộp nguyên liệu ở GĐ3, khi đã có bảng tham chiếu.)

- [ ] **Step 1: Viết test fail**

```java
// IngredientRepositoryTest — @DataJpaTest + Testcontainers
@Test void seedContainsRequiredIngredients() { /* findBySlug("ca-chua"), "trung-ga" có mặt và APPROVED */ }
@Test void nameUniqueIgnoringCase() { /* save "CÀ CHUA" + flush → DataIntegrityViolationException */ }
@Test void searchWithoutDiacriticsFindsVietnamese() {
  assertThat(repo.search("ca chua", Slugs.slugify("ca chua"), null, APPROVED, Limit.of(200)))
    .extracting(Ingredient::getName).contains("Cà chua");
}
@Test void searchExcludesPending() {}

// IngredientServiceTest (Mockito)
@Test void suggestReturnsExistingIgnoringCaseAndSpaces() { /* "cà chua " → created=false, id của "Cà chua" */ }
@Test void suggestCreatesPending() { /* created=true, status PENDING, createdBy = user */ }
@Test void suggestWithSlugCollisionReturnsExisting() { /* "Ca chua" có slug "ca-chua" đã tồn tại → trả cái có sẵn */ }

// IngredientControllerTest — @WebMvcTest
@Test void listIsPublic() {}
@Test @WithMockUser(authorities = "RECIPE_WRITE") void suggestStatusCodes() { /* created → 201, existing → 200 */ }
@Test @WithMockUser(authorities = "RECIPE_WRITE") void memberCannotApprove() { /* PUT /api/admin/ingredients/1 → 403 */ }
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `./mvnw test -Dtest='Ingredient*Test'` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Chạy pass** — Run: `./mvnw verify` — Expected: `BUILD SUCCESS`.
- [ ] **Step 5: Commit** — `git commit -am "feat(catalog): ingredient catalog with suggestions and diacritic-free search"`

---

### Task 4: Frontend — upload ảnh, avatar, admin danh mục

**Files:**
- Create: `frontend/src/api/{media,catalog}.ts`, `src/utils/slugify.ts`, `src/components/ImageUpload.vue`
- Create: `src/pages/admin/{AdminCategoriesPage,AdminTagsPage,AdminIngredientsPage}.vue`
- Modify: `src/pages/AccountPage.vue` (avatar), `src/layouts/AdminLayout.vue` (link "Danh mục", "Tag", "Nguyên liệu" ↔ CATALOG_MANAGE), `src/router/index.ts`
- Test: `src/utils/__tests__/slugify.spec.ts`, `src/components/__tests__/ImageUpload.spec.ts`, `src/pages/admin/__tests__/AdminIngredientsPage.spec.ts`

**Interfaces:**
- Produces:
  ```ts
  export interface MediaRef { id: number; url: string }
  export function uploadImage(file: File): Promise<MediaRef>        // FormData 'file' → POST /media
  export type IngredientGroup = 'VEGETABLE'|'FRUIT'|'MEAT'|'SEAFOOD'|'EGG_DAIRY'|'GRAIN'|'SPICE'|'SAUCE'|'OTHER'
  export const GROUP_LABELS: Record<IngredientGroup, string>        // Rau củ, Trái cây, Thịt, Hải sản, Trứng & sữa, Ngũ cốc, Gia vị, Nước chấm & sốt, Khác
  export interface Ingredient { id: number; name: string; slug: string; group: IngredientGroup; emoji: string|null; status: 'APPROVED'|'PENDING' }
  export function listIngredients(q?: string, group?: IngredientGroup): Promise<Ingredient[]>
  export function suggestIngredient(r: { name: string; group: IngredientGroup; emoji?: string }): Promise<Ingredient>
  export function slugify(text: string): string                     // cùng kết quả với Java Slugs.slugify
  // ImageUpload.vue: props { modelValue: MediaRef | null }, emits 'update:modelValue'
  //   accept="image/jpeg,image/png,image/webp"; file > 5MB → lỗi "Ảnh tối đa 5MB", không gọi API
  ```

- [ ] **Step 1: Viết test fail**

```ts
// slugify.spec.ts — cùng bộ dữ liệu với SlugsTest Java
it.each([['Cà chua','ca-chua'],['Đậu phụ chiên','dau-phu-chien'],['  Phở   bò!! ','pho-bo'],['Bánh mì & pate','banh-mi-pate'],['Gà ĐÔNG TẢO','ga-dong-tao']])(
  'slugify(%s)', (i, o) => expect(slugify(i)).toBe(o))

// ImageUpload.spec.ts
it('rejects files over 5MB without calling API', async () => { /* File 6MB → text "Ảnh tối đa 5MB", uploadImage not called */ })
it('emits MediaRef after upload', async () => {})

// AdminIngredientsPage.spec.ts
it('approve sends status APPROVED', async () => { /* bấm "Duyệt" ở dòng PENDING → PUT /admin/ingredients/7 json.status 'APPROVED' */ })
it('shows IN_USE message on delete conflict', async () => { /* "Nguyên liệu đang được dùng, hãy gộp thay vì xóa" */ })
```

- [ ] **Step 2: Chạy để thấy fail** — Run: `npm run test` — Expected: FAIL.
- [ ] **Step 3: Implement.** Thêm vào `messages.ts`: `MEDIA_TYPE`, `MEDIA_TOO_LARGE`, `NAME_TAKEN`, `IN_USE`.
- [ ] **Step 4: Cổng giai đoạn** — Run: `cd backend && ./mvnw verify && cd ../frontend && npm run test && npm run type-check` — Expected: tất cả xanh.
- [ ] **Step 5: Kiểm tra tay** — đổi avatar ở `/account`; admin duyệt một nguyên liệu PENDING.
- [ ] **Step 6: Commit** — `git commit -am "feat(frontend): image upload, avatar and catalog admin"`
