package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@NotBlank(message = "Bắt buộc") String email) {
}
