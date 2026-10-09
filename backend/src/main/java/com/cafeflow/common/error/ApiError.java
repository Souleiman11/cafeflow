package com.cafeflow.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Structured error body returned by every failing {@code /api/v1} request
 * (MASTER_SPEC section 61). Never carries stack traces or internal exception details.
 *
 * @param fieldErrors populated only for {@link ErrorCode#VALIDATION_ERROR}; otherwise empty
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    /** A single invalid request field. */
    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, ErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), status, code.name(), message, path, List.of());
    }
}
