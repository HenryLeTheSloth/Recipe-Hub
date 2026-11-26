package com.recipehub.recipe;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    Optional<Recipe> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Recipe> findByAuthor_IdAndStatusIn(Long authorId, Collection<RecipeStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Recipe> findByStatus(RecipeStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);
}
