package com.recipehub.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException e) {
        return problem(e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f ->
                errors.putIfAbsent(f.getField(), f.getDefaultMessage() != null ? f.getDefaultMessage() : "Không hợp lệ"));
        ProblemDetail p = problem(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ");
        p.setProperty("errors", errors);
        return p;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return problem(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException e) {
        return problem(ErrorCode.MEDIA_TOO_LARGE, "Ảnh tối đa 5MB");
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(code.status(), detail);
        p.setProperty("code", code.name());
        return p;
    }
}
