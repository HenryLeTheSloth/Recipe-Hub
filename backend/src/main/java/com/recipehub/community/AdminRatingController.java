package com.recipehub.community;

import com.recipehub.community.dto.HiddenRequest;
import com.recipehub.community.dto.RatingView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ratings")
@PreAuthorize("hasAuthority('CONTENT_MODERATE')")
public class AdminRatingController {

    private final RatingService service;

    public AdminRatingController(RatingService service) {
        this.service = service;
    }

    @PatchMapping("/{id}")
    public RatingView setHidden(@PathVariable Long id, @RequestBody HiddenRequest req) {
        return service.setHidden(id, req.hidden());
    }
}
