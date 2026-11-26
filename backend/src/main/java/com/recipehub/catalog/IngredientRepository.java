package com.recipehub.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByNameIgnoreCase(String name);

    Optional<Ingredient> findBySlug(String slug);

    List<Ingredient> findAllBySlugInAndStatus(Collection<String> slugs, IngredientStatus status);

    /** q khớp tên (có dấu) hoặc qSlug khớp slug (không dấu). q/qSlug không được null — truyền "". */
    @Query("""
            select i from Ingredient i
            where i.status = :status
              and (lower(i.name) like lower(concat('%', :q, '%')) or i.slug like concat('%', :qSlug, '%'))
              and (:group is null or i.ingredientGroup = :group)
            order by i.name
            """)
    List<Ingredient> search(String q, String qSlug, IngredientGroup group, IngredientStatus status, Limit limit);

    @Query("""
            select i from Ingredient i
            where (:status is null or i.status = :status)
              and (lower(i.name) like lower(concat('%', :q, '%')) or i.slug like concat('%', :qSlug, '%'))
            order by i.status desc, i.name
            """)
    Page<Ingredient> adminSearch(IngredientStatus status, String q, String qSlug, Pageable pageable);

    // Gộp nguyên liệu: công thức có cả hai → ghi lượng của dòng nguồn vào ghi chú dòng đích rồi bỏ dòng nguồn;
    // phần còn lại trỏ sang đích; cuối cùng xóa nguồn.
    @Modifying
    @Query(nativeQuery = true, value = """
            update recipe_ingredients t set note = left(concat_ws('; ', t.note, concat_ws(' ', 'thêm',
                trim_scale(s.quantity)::text,
                case s.unit when 'TSP' then 'thìa cà phê' when 'TBSP' then 'thìa canh' when 'CUP' then 'cốc'
                            when 'PIECE' then 'cái/quả' when 'PINCH' then 'nhúm' else lower(s.unit) end,
                s.note)), 100)
            from recipe_ingredients s
            where s.recipe_id = t.recipe_id and s.ingredient_id = :src and t.ingredient_id = :tgt
            """)
    void foldDuplicateLinesIntoTargetNote(Long src, Long tgt);

    @Modifying
    @Query(nativeQuery = true, value = """
            delete from recipe_ingredients s where s.ingredient_id = :src
              and exists (select 1 from recipe_ingredients t where t.recipe_id = s.recipe_id and t.ingredient_id = :tgt)
            """)
    void dropRecipeLinesDuplicatedByTarget(Long src, Long tgt);

    @Modifying
    @Query(nativeQuery = true, value = "update recipe_ingredients set ingredient_id = :tgt where ingredient_id = :src")
    void repointRecipeLines(Long src, Long tgt);

    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = "delete from ingredients where id = :src")
    void deleteNative(Long src);
}
