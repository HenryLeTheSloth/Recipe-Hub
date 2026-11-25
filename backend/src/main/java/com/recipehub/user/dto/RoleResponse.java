package com.recipehub.user.dto;

import com.recipehub.user.Permission;
import com.recipehub.user.Role;
import java.util.List;

public record RoleResponse(Long id, String code, String name, String description, boolean system,
                           List<String> permissionCodes, long userCount) {

    public static RoleResponse from(Role r, long userCount) {
        return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSystem(),
                r.getPermissions().stream().map(Permission::getCode).sorted().toList(), userCount);
    }
}
