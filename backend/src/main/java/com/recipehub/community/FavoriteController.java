package com.recipehub.community;

import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.user.AppUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @PutMapping("/api/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COMMUNITY_PARTICIPATE')")
    public void add(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.add(actor, id);
    }

    @DeleteMapping("/api/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COMMUNITY_PARTICIPATE')")
    public void remove(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.remove(actor, id);
    }

    @GetMapping("/api/me/favorites")
    public PagedModel<RecipeCard> mine(@AuthenticationPrincipal AppUserDetails actor, Pageable pageable) {
        return new PagedModel<>(service.listMine(actor, pageable));
    }
}
