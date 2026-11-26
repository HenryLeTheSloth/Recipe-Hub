package com.recipehub.recipe;

import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.recipe.dto.RecipeRequest;
import com.recipehub.user.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RECIPE_WRITE')")
    public RecipeDetail create(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody RecipeRequest r) {
        return service.create(principal, r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RECIPE_WRITE')")
    public RecipeDetail update(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                               @Valid @RequestBody RecipeRequest r) {
        return service.update(principal, id, r);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('RECIPE_WRITE')")
    public RecipeDetail submit(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return service.submit(principal, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        service.delete(principal, id);
    }
}
