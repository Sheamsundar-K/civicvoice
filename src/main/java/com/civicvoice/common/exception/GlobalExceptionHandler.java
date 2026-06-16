package com.civicvoice.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.CONFLICT, "Duplicate Resource", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", ex.getMessage(), req);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiError> handleFileUpload(FileUploadException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, "File Upload Failed", ex.getMessage(), req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                "Upload exceeds maximum allowed size", req);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest req) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
            .filter(e -> e instanceof FieldError)
            .map(e -> (FieldError) e)
            .map(fe -> ApiError.FieldViolation.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue())
                .build())
            .toList();

        ApiError error = ApiError.builder()
            .type("https://civicvoice.gov/errors/validation-failed")
            .title("Validation Failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("One or more fields failed validation")
            .instance(req.getRequestURI())
            .timestamp(OffsetDateTime.now())
            .violations(violations)
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied",
                "You do not have permission to perform this action", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication Required",
                "Valid authentication credentials are required", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", req);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String title,
                                                    String detail, HttpServletRequest req) {
        ApiError error = ApiError.builder()
            .type("https://civicvoice.gov/errors/" + title.toLowerCase().replace(" ", "-"))
            .title(title)
            .status(status.value())
            .detail(detail)
            .instance(req.getRequestURI())
            .timestamp(OffsetDateTime.now())
            .build();
        return ResponseEntity.status(status).body(error);
    }
}
