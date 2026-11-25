package com.recipehub.common;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionBecomesProblemWithCode() {
        ProblemDetail p = handler.handleApi(new ApiException(ErrorCode.EMAIL_TAKEN, "Email đã được dùng"));
        assertThat(p.getStatus()).isEqualTo(409);
        assertThat(p.getDetail()).isEqualTo("Email đã được dùng");
        assertThat(p.getProperties()).containsEntry("code", "EMAIL_TAKEN");
    }

    @Test
    void validationErrorListsFields() {
        var binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "email", "Bắt buộc"));
        ProblemDetail p = handler.handleValidation(new MethodArgumentNotValidException(null, binding));
        assertThat(p.getStatus()).isEqualTo(400);
        assertThat(p.getProperties()).containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("errors", Map.of("email", "Bắt buộc"));
    }

    @Test
    void accessDeniedIs403() {
        ProblemDetail p = handler.handleAccessDenied(new AccessDeniedException("x"));
        assertThat(p.getStatus()).isEqualTo(403);
        assertThat(p.getProperties()).containsEntry("code", "FORBIDDEN");
    }

    @Test
    void maxUploadSizeMapsTo413() {
        ProblemDetail p = handler.handleMaxUpload(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(5L * 1024 * 1024));
        assertThat(p.getStatus()).isEqualTo(413);
        assertThat(p.getProperties()).containsEntry("code", "MEDIA_TOO_LARGE");
    }
}
