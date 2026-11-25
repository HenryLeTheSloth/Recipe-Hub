package com.recipehub.user.dto;

import java.util.List;

public record MeResponse(Long id, String email, String username, String displayName, String avatarUrl,
                         List<String> roles, List<String> permissions) {
}
