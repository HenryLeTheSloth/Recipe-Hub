package com.recipehub.user;

import com.recipehub.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordsTest {

    @Test
    void acceptsEightAsciiChars() {
        assertThatNoException().isThrownBy(() -> Passwords.validate("abcdefgh"));
    }

    @Test
    void rejectsSeven() {
        assertThatThrownBy(() -> Passwords.validate("abcdefg"))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Passwords.validate(null))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void acceptsExactly72Bytes() {
        assertThatNoException().isThrownBy(() -> Passwords.validate("ệ".repeat(24)));
    }

    @Test
    void rejects73PlusBytes() {
        assertThatThrownBy(() -> Passwords.validate("ệ".repeat(25)))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PASSWORD);
    }
}
