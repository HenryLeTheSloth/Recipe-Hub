package com.recipehub.community;

import com.recipehub.community.dto.CommentRequest;
import com.recipehub.community.dto.CommentView;
import com.recipehub.user.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping("/api/recipes/{id}/comments")
    public List<CommentView> list(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails viewer) {
        return service.list(id, viewer);
    }

    @PostMapping("/api/recipes/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COMMUNITY_PARTICIPATE')")
    public CommentView create(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                              @Valid @RequestBody CommentRequest req) {
        return service.create(actor, id, req);
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.delete(actor, id);
    }
}
