package com.recipehub.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserRolesRequest(@NotEmpty(message = "Cần ít nhất một vai trò") Set<String> roleCodes) {
}
