package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank(message = "Bắt buộc") String token,
                                   @NotBlank(message = "Bắt buộc") String newPassword) {
}
