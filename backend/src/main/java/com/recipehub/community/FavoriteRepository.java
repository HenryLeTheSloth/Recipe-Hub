package com.recipehub.community;

import com.recipehub.recipe.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    @Query(value = "select f.recipe from Favorite f where f.user.id = :userId and f.recipe.status = 'PUBLISHED' "
            + "order by f.createdAt desc, f.recipe.id desc",
            countQuery = "select count(f) from Favorite f where f.user.id = :userId and f.recipe.status = 'PUBLISHED'")
    Page<Recipe> publishedBy(@Param("userId") Long userId, Pageable pageable);
}
