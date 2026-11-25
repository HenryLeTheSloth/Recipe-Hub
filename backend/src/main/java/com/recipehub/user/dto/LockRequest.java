package com.recipehub.user.dto;

import jakarta.validation.constraints.NotNull;

public record LockRequest(@NotNull(message = "Bắt buộc") Boolean locked) {
}
