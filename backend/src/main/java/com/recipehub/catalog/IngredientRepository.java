package com.recipehub.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
