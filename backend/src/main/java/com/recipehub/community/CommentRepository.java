package com.recipehub.community;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findTop200ByRecipe_IdAndParentIsNullOrderByCreatedAtAscIdAsc(Long recipeId);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByParent_IdInOrderByCreatedAtAscIdAsc(Collection<Long> parentIds);
}
