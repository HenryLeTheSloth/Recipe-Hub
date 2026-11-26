package com.recipehub.community;

import com.recipehub.community.dto.RatingRequest;
import com.recipehub.community.dto.RatingView;
import com.recipehub.user.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/{id}")
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @GetMapping("/ratings")
    public PagedModel<RatingView> list(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails viewer,
                                       Pageable pageable) {
        return new PagedModel<>(service.list(id, viewer,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    @PutMapping("/my-rating")
    @PreAuthorize("hasAuthority('COMMUNITY_PARTICIPATE')")
    public RatingView rate(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                           @Valid @RequestBody RatingRequest req) {
        return service.upsert(actor, id, req);
    }

    @DeleteMapping("/my-rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COMMUNITY_PARTICIPATE')")
    public void deleteMine(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.deleteMine(actor, id);
    }
}
