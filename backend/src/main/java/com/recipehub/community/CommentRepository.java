package com.recipehub.community;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findTop200ByRecipe_IdAndParentIsNullOrderByCreatedAtAscIdAsc(Long recipeId);

    List<Comment> findByParent_IdInOrderByCreatedAtAscIdAsc(Collection<Long> parentIds);
}
