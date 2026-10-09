package com.cafeflow.common.error;

/** Stable, machine-readable error codes returned in {@link ApiError#code()}. */
public enum ErrorCode {
    UNAUTHENTICATED,
    FORBIDDEN,
    INVALID_REQUEST,
    VALIDATION_ERROR,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    INTERNAL_ERROR
}
