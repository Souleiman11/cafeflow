# Security baseline (Phase 0)

- Deny-by-default HTTP policy (`SecurityConfig`): only health probes and `/actuator/info` are public.
- Stateless API; no sessions. CSRF is disabled because there is no cookie authentication yet -
  **Phase 1 must revisit this** if refresh tokens are delivered by cookie.
- Errors never leak internals (`GlobalExceptionHandler`); security-filter failures use the same format.
- `X-Request-Id` accepted only if it matches `[A-Za-z0-9-]{8,64}` (log-injection protection).
- Containers run as non-root (UID 10001 / nginx-unprivileged); Kubernetes pods drop all capabilities,
  disallow privilege escalation, use `RuntimeDefault` seccomp.
- No secrets in images or git. Required settings (`DATABASE_*`, `REDIS_URL`) have no defaults in
  `application.yml`, so the app fails fast. `.env` and `secrets.yaml` are git-ignored;
  `infrastructure/kubernetes/secrets.example.yaml` is a placeholder template.
- Nginx forwards only `/api/` and `/actuator/health/` to the backend.

Mandatory security tests (spec section 46) are introduced as their features land; the
deny-by-default and error-leak tests exist now.

## Dependency hygiene
- Python runtime dependencies are pinned exactly (including transitive) and audited with `pip-audit` in CI.
- Frontend: `npm ci` from the lockfile; `npm audit --audit-level=high` in CI.
- Java dependencies are managed by the Spring Boot BOM; audit them with the CI `security-scan` job once the build can resolve them.
