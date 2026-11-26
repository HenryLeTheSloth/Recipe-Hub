package com.recipehub.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    USERNAME_TAKEN(HttpStatus.CONFLICT),
    ROLE_CODE_TAKEN(HttpStatus.CONFLICT),
    ROLE_IN_USE(HttpStatus.CONFLICT),
    SYSTEM_ROLE(HttpStatus.CONFLICT),
    LAST_ADMIN(HttpStatus.CONFLICT),
    SELF_ACTION(HttpStatus.CONFLICT),
    USER_HAS_CONTENT(HttpStatus.CONFLICT),
    NAME_TAKEN(HttpStatus.CONFLICT),
    IN_USE(HttpStatus.CONFLICT),
    MEDIA_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
    MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_TRANSITION(HttpStatus.CONFLICT),
    RECIPE_INCOMPLETE(HttpStatus.BAD_REQUEST),
    DUPLICATE_INGREDIENT(HttpStatus.BAD_REQUEST),
    COMMENT_DEPTH(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
