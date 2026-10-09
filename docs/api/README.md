# API

Base path `/api/v1`, JSON, errors as `ApiError`:

```json
{ "timestamp": "...", "status": 403, "code": "FORBIDDEN",
  "message": "You do not have permission to perform this action.",
  "path": "/api/v1/...", "fieldErrors": [] }
```

`fieldErrors` is populated only for `VALIDATION_ERROR`. Codes are defined in
`com.cafeflow.common.error.ErrorCode`; stack traces and exception details are never returned.

**Phase 0 has no business endpoints.** OpenAPI (springdoc) is added in Phase 1 together with the
first endpoints, pinned to the release line compatible with Spring Boot 4 (see ADR-004).
Every request also carries an `X-Request-Id` response header for log correlation.
