package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank(message = "Bắt buộc") String currentPassword,
                                    @NotBlank(message = "Bắt buộc") String newPassword) {
}
