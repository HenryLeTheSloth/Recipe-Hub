package com.recipehub.recipe;

import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.recipe.dto.RecipeSearch;
import com.recipehub.user.AppUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class PublicRecipeController {

    private final RecipeQueryService service;

    public PublicRecipeController(RecipeQueryService service) {
        this.service = service;
    }

    /** ?q&category&tag&ingredientIds=1,2&sort&page&size. Tham số sort là của RecipeSearch, không phải Pageable. */
    @GetMapping
    public PagedModel<RecipeCard> search(RecipeSearch search, Pageable pageable) {
        return new PagedModel<>(service.search(search, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    @GetMapping("/{slug}")
    public RecipeDetail get(@PathVariable String slug, @AuthenticationPrincipal AppUserDetails viewer) {
        return service.getBySlug(slug, viewer);
    }
}
