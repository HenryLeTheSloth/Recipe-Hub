package com.recipehub.user.dto;

import com.recipehub.user.Role;
import com.recipehub.user.User;
import java.time.Instant;
import java.util.List;

public record AdminUserResponse(Long id, String email, String username, String displayName, boolean locked,
                                boolean emailVerified, List<String> roles, Instant createdAt) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getDisplayName(), u.isLocked(),
                u.getEmailVerifiedAt() != null, u.getRoles().stream().map(Role::getCode).sorted().toList(),
                u.getCreatedAt());
    }
}
