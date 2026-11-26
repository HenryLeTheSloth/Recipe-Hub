package com.recipehub.community;

import com.recipehub.community.dto.CommentView;
import com.recipehub.community.dto.HiddenRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasAuthority('CONTENT_MODERATE')")
public class AdminCommentController {

    private final CommentService service;

    public AdminCommentController(CommentService service) {
        this.service = service;
    }

    @PatchMapping("/{id}")
    public CommentView setHidden(@PathVariable Long id, @RequestBody HiddenRequest req) {
        return service.setHidden(id, req.hidden());
    }
}
