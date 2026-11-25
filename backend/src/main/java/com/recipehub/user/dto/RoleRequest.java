package com.recipehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** {@code code} chỉ dùng khi tạo; PUT bỏ qua. */
public record RoleRequest(
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,30}$", message = "3–31 ký tự IN HOA, số hoặc _, bắt đầu bằng chữ")
        String code,
        @NotBlank(message = "Bắt buộc") @Size(max = 50, message = "Tối đa 50 ký tự") String name,
        @Size(max = 200, message = "Tối đa 200 ký tự") String description,
        @NotNull(message = "Bắt buộc") Set<String> permissionCodes) {
}
