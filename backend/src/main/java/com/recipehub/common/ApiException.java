package com.recipehub.common;

public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    // Cho assertThat(...).hasFieldOrPropertyWithValue("code", ...)
    public ErrorCode getCode() {
        return code;
    }
}
