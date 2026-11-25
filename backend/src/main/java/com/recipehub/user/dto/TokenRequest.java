package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank(message = "Bắt buộc") String token) {
}
