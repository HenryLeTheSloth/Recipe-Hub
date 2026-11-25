package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.nio.charset.StandardCharsets;

public final class Passwords {

    private Passwords() {
    }

    // BCrypt chỉ dùng 72 byte đầu → giới hạn theo byte UTF-8, không theo ký tự.
    public static void validate(String raw) {
        if (raw == null || raw.length() < 8 || raw.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD, "Mật khẩu phải có ít nhất 8 ký tự và không quá 72 byte");
        }
    }
}
