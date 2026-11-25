package com.recipehub.recipe;

import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.user.AppUserDetails;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/recipes")
public class MyRecipeController {

    private final RecipeService service;

    public MyRecipeController(RecipeService service) {
        this.service = service;
    }

    /** status=DRAFT,PENDING; bỏ trống = mọi trạng thái. */
    @GetMapping
    public PagedModel<RecipeCard> list(@AuthenticationPrincipal AppUserDetails principal,
                                       @RequestParam(required = false) Set<RecipeStatus> status, Pageable pageable) {
        return new PagedModel<>(service.listMine(principal, status == null ? Set.of() : status,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    @GetMapping("/{id}")
    public RecipeDetail get(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return service.getMine(principal, id);
    }
}
