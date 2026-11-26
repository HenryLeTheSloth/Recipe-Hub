package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.community.dto.CommentRequest;
import com.recipehub.community.dto.CommentView;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeMapper;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeService;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository comments;
    private final RecipeRepository recipes;
    private final UserRepository users;
    private final RecipeMapper mapper;

    public CommentService(CommentRepository comments, RecipeRepository recipes, UserRepository users,
                          RecipeMapper mapper) {
        this.comments = comments;
        this.recipes = recipes;
        this.users = users;
        this.mapper = mapper;
    }

    /** Bình luận ẩn (và mọi trả lời của nó) chỉ moderator thấy. */
    @Transactional(readOnly = true)
    public List<CommentView> list(Long recipeId, @Nullable AppUserDetails viewer) {
        published(recipeId);
        boolean moderator = viewer != null && viewer.can(RecipeService.MODERATE);
        List<Comment> roots = comments.findTop200ByRecipe_IdAndParentIsNullOrderByCreatedAtAscIdAsc(recipeId).stream()
                .filter(c -> moderator || !c.isHidden()).toList();
        if (roots.isEmpty()) return List.of();
        Map<Long, List<CommentView>> replies = comments
                .findByParent_IdInOrderByCreatedAtAscIdAsc(roots.stream().map(Comment::getId).toList()).stream()
                .filter(c -> moderator || !c.isHidden())
                .collect(Collectors.groupingBy(c -> c.getParent().getId(),
                        Collectors.mapping(c -> view(c, List.of()), Collectors.toList())));
        return roots.stream().map(c -> view(c, replies.getOrDefault(c.getId(), List.of()))).toList();
    }

    public CommentView create(AppUserDetails actor, Long recipeId, CommentRequest req) {
        Recipe recipe = published(recipeId);
        Comment parent = null;
        if (req.parentId() != null) {
            parent = comments.findById(req.parentId()).filter(p -> p.getRecipe().getId().equals(recipeId))
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Bình luận gốc không hợp lệ"));
            if (parent.getParent() != null) {
                throw new ApiException(ErrorCode.COMMENT_DEPTH, "Chỉ được trả lời bình luận gốc");
            }
        }
        return view(comments.save(new Comment(recipe, users.getReferenceById(actor.id()), parent, req.content())),
                List.of());
    }

    public void delete(AppUserDetails actor, Long commentId) {
        Comment c = find(commentId);
        if (!c.getAuthor().getId().equals(actor.id()) && !actor.can(RecipeService.MODERATE)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Không có quyền xóa bình luận này");
        }
        comments.delete(c); // trả lời xóa theo bằng ON DELETE CASCADE của DB
    }

    public CommentView setHidden(Long commentId, boolean hidden) {
        Comment c = find(commentId);
        c.setHidden(hidden);
        return view(c, List.of());
    }

    private Comment find(Long id) {
        return comments.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy bình luận"));
    }

    private Recipe published(Long recipeId) {
        return recipes.findById(recipeId).filter(r -> r.getStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy công thức"));
    }

    private CommentView view(Comment c, List<CommentView> replies) {
        return new CommentView(c.getId(), c.getContent(), mapper.author(c.getAuthor()), c.isHidden(),
                c.getCreatedAt(), replies);
    }
}
