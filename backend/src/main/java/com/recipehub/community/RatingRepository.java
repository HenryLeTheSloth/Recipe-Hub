package com.recipehub.community;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    @EntityGraph(attributePaths = "user")
    Page<Rating> findByRecipe_IdAndHiddenFalse(Long recipeId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Rating> findByRecipe_Id(Long recipeId, Pageable pageable);

    @Query("select new com.recipehub.community.RatingStats(r.recipe.id, avg(r.stars), count(r)) "
            + "from Rating r where r.recipe.id in :ids and r.hidden = false group by r.recipe.id")
    List<RatingStats> statsFor(Collection<Long> ids);
}
