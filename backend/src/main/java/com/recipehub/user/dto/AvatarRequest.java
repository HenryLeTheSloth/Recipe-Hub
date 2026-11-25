package com.recipehub.user.dto;

import jakarta.validation.constraints.NotNull;

public record AvatarRequest(@NotNull(message = "Bắt buộc") Long mediaId) {
}
