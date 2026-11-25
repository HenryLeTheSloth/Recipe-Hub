package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Bắt buộc") String email,
                           @NotBlank(message = "Bắt buộc") String password) {
}
