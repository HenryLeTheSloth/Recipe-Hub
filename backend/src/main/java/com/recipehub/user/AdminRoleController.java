package com.recipehub.user;

import com.recipehub.user.dto.PermissionResponse;
import com.recipehub.user.dto.RoleRequest;
import com.recipehub.user.dto.RoleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class AdminRoleController {

    private final RoleService service;

    public AdminRoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    public List<RoleResponse> list() {
        return service.list();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody RoleRequest r) {
        return service.create(r);
    }

    @PutMapping("/roles/{id}")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() {
        return service.permissions();
    }
}
