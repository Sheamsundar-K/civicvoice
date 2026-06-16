package com.civicvoice.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * RFC 7807 Problem+JSON compliant error response.
 * Never leaks stack traces to clients.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final OffsetDateTime timestamp;
    private final List<FieldViolation> violations;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldViolation {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }
}
