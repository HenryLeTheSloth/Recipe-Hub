package com.recipehub.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Mọi lỗi /api là ProblemDetail có `code` — kể cả lỗi request của Spring (sai kiểu tham số, JSON hỏng…). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException e) {
        return problem(e.code(), e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(handleValidation(e));
    }

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

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(ErrorCode.MEDIA_TOO_LARGE.status()).body(handleMaxUpload(e));
    }

    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException e) {
        return problem(ErrorCode.MEDIA_TOO_LARGE, "Ảnh tối đa 5MB");
    }

    /** Lỗi request còn lại của Spring: giữ ProblemDetail mặc định, chỉ gắn thêm `code`. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail p && status.is4xxClientError()) {
            p.setProperty("code", (status.value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.VALIDATION_ERROR).name());
        }
        return super.handleExceptionInternal(e, body, headers, status, request);
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(code.status(), detail);
        p.setProperty("code", code.name());
        return p;
    }
}
