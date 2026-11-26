package com.recipehub.recipe;

import com.recipehub.recipe.dto.ReasonRequest;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.user.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/recipes")
@PreAuthorize("hasAuthority('CONTENT_MODERATE')")
public class AdminRecipeController {

    private final RecipeModerationService service;

    public AdminRecipeController(RecipeModerationService service) {
        this.service = service;
    }

    @GetMapping
    public PagedModel<RecipeCard> queue(@RequestParam(required = false) RecipeStatus status, Pageable pageable) {
        return new PagedModel<>(service.queue(status, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    @GetMapping("/{id}")
    public RecipeDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/approve")
    public RecipeDetail approve(@AuthenticationPrincipal AppUserDetails mod, @PathVariable Long id) {
        return service.approve(mod, id);
    }

    @PostMapping("/{id}/reject")
    public RecipeDetail reject(@AuthenticationPrincipal AppUserDetails mod, @PathVariable Long id,
                               @Valid @RequestBody ReasonRequest r) {
        return service.reject(mod, id, r.reason());
    }

    @PostMapping("/{id}/hide")
    public RecipeDetail hide(@AuthenticationPrincipal AppUserDetails mod, @PathVariable Long id,
                             @Valid @RequestBody ReasonRequest r) {
        return service.hide(mod, id, r.reason());
    }

    @PostMapping("/{id}/unhide")
    public RecipeDetail unhide(@AuthenticationPrincipal AppUserDetails mod, @PathVariable Long id) {
        return service.unhide(mod, id);
    }
}
