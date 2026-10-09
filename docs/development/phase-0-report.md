# Phase 0 Completion Report

**Phase:** 0 - Foundation  **Status:** PARTIALLY VERIFIED (see "Not verified in the authoring environment")

## Verified (executed, passing)
| Area | Evidence |
|---|---|
| ML service | `ruff` clean; `pytest` 3/3; booted with uvicorn, `/health` -> 200 |
| Frontend | `ng lint` clean; `ng test` 7/7 (Vitest); production build OK |
| nginx (frontend image config) | rendered template; `nginx -t` OK; SPA fallback, `/api` + readiness proxied, other `/actuator/*` not proxied, security headers incl. CSP |
| Docker Compose file | `docker compose config` valid (Compose v5.6.0) |
| Kubernetes | `kustomize build` + `kubeconform -strict` (K8s 1.31): 9/9 valid; secret template valid |
| Terraform | OpenTofu 1.13 `fmt -check`, `validate`, `plan` OK |
| CI workflow | `actionlint` clean |
| Dockerfiles | `hadolint` clean (DL3025 suppressed on HEALTHCHECK, documented) |

## Not verified in the authoring environment
| Item | Why | How it gets verified |
|---|---|---|
| **Backend compile + tests** | Maven Central unreachable from the sandbox | CI `backend` job (`./mvnw verify`); locally `./mvnw test` |
| **Docker image builds** | No Docker daemon | CI `docker-and-smoke` job |
| **`docker compose up` end-to-end + `scripts/smoke-test.sh`** | No Docker daemon | same CI job, or locally |
| **Testcontainers `FoundationIT`** | needs Docker + Maven | `./mvnw verify` |
| Trivy scan, `kubectl` dry-run | tools unavailable | CI `security-scan` / cluster |

The backend was only syntax-checked with `javac` (no errors other than unresolved dependencies).
It targets Spring Boot 4.1.1 / Testcontainers 2.x APIs from the migration guides; if a coordinate
or import differs, the first `./mvnw test` will show it immediately.

## Known limitations
- Phase 0 has no business features, authentication or migrations by design.
- `trivy-action@0.28.0` pin and image tags (`nginx-unprivileged:1.27`, `postgres:17`, `redis:7`) should be
  reviewed for newer patch versions.
- Kubernetes images use placeholder names/tags; ingress host is `cafeflow.example.com`.

## Next phase
Phase 1 - Identity + Tenant (blocked on Phase 0 CI being green).
