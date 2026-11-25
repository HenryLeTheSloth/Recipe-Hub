package com.recipehub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Bắt buộc") @Email(message = "Email không hợp lệ")
        @Size(max = 254, message = "Tối đa 254 ký tự") String email,
        @NotNull(message = "Bắt buộc")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$", message = "3–30 ký tự: chữ, số hoặc _") String username,
        @NotBlank(message = "Bắt buộc") @Size(max = 50, message = "Tối đa 50 ký tự") String displayName,
        @NotBlank(message = "Bắt buộc") String password) {

    public RegisterRequest {
        email = email == null ? null : email.trim(); // "  a@x.com " hợp lệ; @Email kiểm trên giá trị đã trim
    }
}
