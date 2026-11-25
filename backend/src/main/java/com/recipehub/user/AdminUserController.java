package com.recipehub.user;

import com.recipehub.user.dto.AdminUserResponse;
import com.recipehub.user.dto.LockRequest;
import com.recipehub.user.dto.UserRolesRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public PagedModel<AdminUserResponse> list(@RequestParam(defaultValue = "") String q, Pageable pageable) {
        return new PagedModel<>(service.list(q, pageable));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public AdminUserResponse lock(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                                  @Valid @RequestBody LockRequest r) {
        return service.setLocked(actor, id, r.locked());
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public AdminUserResponse roles(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                                   @Valid @RequestBody UserRolesRequest r) {
        return service.setRoles(actor, id, r.roleCodes());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.delete(actor, id);
    }
}
