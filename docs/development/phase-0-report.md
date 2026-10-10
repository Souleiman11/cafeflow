# Phase 0 Verification and Hardening Report

**Phase:** 0 - Foundation  **Overall status: NOT COMPLETE - backend and Docker stack are unverified.**
Everything that could run in the authoring sandbox was run and passes. The Spring Boot backend could
**not** be compiled or tested, and the Docker stack could **not** be built or started. Phase 1 must not
start until CI (`.github/workflows/ci.yml`) is green on a machine with Maven Central and Docker access.

Legend: **PASS** = executed successfully here. **FAIL** = executed and failed. **NOT RUN** = could not be
executed here (reason given). Nothing below is marked PASS unless the command ran and succeeded.

## Correction to the previous report
The earlier report said the backend was "syntax-checked with javac with no errors". **That was wrong:**
`javac` was not installed, the command failed with "not found", and my filter hid the message. The probe
has now been run properly (JDK 21.0.12, see item 2). Also, the earlier secret scan used `git grep` before
anything was committed, so it was vacuous; it has been replaced with gitleaks (item 9).

## Environment constraints (why items are NOT RUN)
| Constraint | Evidence |
|---|---|
| Maven Central unreachable | `curl` to repo.maven.apache.org / repo1.maven.org / Spring / Google / Gradle repos -> no connection; `mvn` reports `Unknown host repo.maven.apache.org` |
| No Docker daemon | `dockerd` not installed, no `/var/run/docker.sock` |
| Container registries unreachable | docker.io, ghcr.io, quay.io, mcr.microsoft.com -> no connection (base images cannot be pulled even with a daemon) |
| npm, PyPI, GitHub releases, Ubuntu apt reachable | used for the checks that did run |

## Results by requested item

### 1. Spring Boot and Java versions - PARTIAL
| Item | Value | Status |
|---|---|---|
| Spring Boot parent | `4.1.1` (`backend/pom.xml`) | Release existence confirmed from spring.io ("Spring Boot 4.1.1 available now", 2026-08-20) |
| Java | 21 (`java.version`, Dockerfiles `temurin 21`, CI `temurin 21`). Sandbox JDK: 21.0.12 | Declared consistently; only the JDK was executed |
| Managed dependency versions (Spring Framework, Hibernate, Flyway, Testcontainers, ...) | **Not resolved** - Maven could not run | NOT RUN |
| Indirect evidence | A secondary blog states Boot 4.1.1 manages Flyway 12.4.0 and Testcontainers 2.0.5. The only primary BOM I could read was **4.2.0-M2** (not 4.1.1): it lists the same artifact names I use (`spring-boot-starter-webmvc`, `-webmvc-test`, `-security-test`, `-flyway`, `-validation`, `-actuator`, `-data-jpa`, `-data-redis`, `flyway-database-postgresql`) and Testcontainers 2.0.5 | Supports, does not prove, that the coordinates are right for 4.1.1 |
| Maven wrapper | 3.3.4 script, distribution Maven 3.9.16 (copied from an upstream project; its existence was not verified because the download is blocked) | NOT VERIFIED |
| `testcontainers-postgresql` artifact | Assumed managed via Testcontainers BOM; not visible in the truncated BOM I read | NOT VERIFIED |

Frontend (resolved by `package-lock.json`, installed with `npm ci`): Angular 22.2.x, TypeScript 6.0.x,
Vitest 5, jsdom 30, angular-eslint 22.5.0. Node >= 22.22.3 is required by the Angular 22 CLI.

### 2. Backend compile and Maven tests - NOT RUN (blocked)
- `./mvnw -B test` -> **FAIL (environment):** `wget: Failed to fetch https://repo.maven.apache.org/.../apache-maven-3.9.16-bin.zip`.
- apt Maven 3.8.7: `pom.xml` **parsed successfully** (well-formed), then failed with
  `Non-resolvable parent POM ... spring-boot-starter-parent:pom:4.1.1 ... Unknown host repo.maven.apache.org`.
- `javac` 21 over all 25 sources without Spring on the classpath: 312 errors, of which 196 "cannot find symbol",
  97 "package does not exist", 16 "static import only from classes and interfaces" and 3 "method does not
  override" - **all consequences of unresolved Spring types** (the 3 override errors are
  `RequestIdFilter`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, whose supertypes are unresolved).
  **No syntax errors were found, but this says nothing about API correctness.** The backend is
  **uncompiled and untested**.
- Nothing was changed in the backend in this pass, because no actual failure could be observed.

First things to check when CI runs, in order of my own uncertainty: (1) `org.testcontainers.postgresql.PostgreSQLContainer`
and `GenericContainer` packages under Testcontainers 2.x; (2) `NoResourceFoundException` /
`NoHandlerFoundException` imports in `GlobalExceptionHandler`; (3) the `handlerExceptionResolver` bean qualifier
in the two security handlers; (4) `SecurityBaselineTest.healthProbesArePublic` assumes an unmapped path returns 404;
(5) `FoundationIT` expects `local.server.port` and ECS structured logging to work with Boot 4.1.

### 3. PostgreSQL, Flyway, Redis startup configuration - PARTIAL
Run against real servers started with the same settings as `docker-compose.yml` (PostgreSQL **16.15**,
Redis **7.0.15** from apt; compose pins PostgreSQL 17 / Redis 7):
- PASS: `pg_isready -U cafeflow -d cafeflow` (the compose healthcheck command) reports accepting connections.
- PASS: SCRAM password auth works with the configured credentials; a wrong password is rejected;
  the app role can create/drop tables in `public` (what Flyway needs).
- PASS: Redis started with the compose command (`--requirepass`, `--save ''`, `--appendonly no`); healthcheck
  `REDISCLI_AUTH=... redis-cli ping` returns PONG; no password -> `NOAUTH`; wrong password -> `WRONGPASS`;
  persistence confirmed off.
- PASS: the URL shape used for `REDIS_URL` (`redis://:<password>@host:port`) authenticates with a real client;
  the same URL without a password is rejected.
- PASS (static): `application.yml` parses; the four variables with no default (`DATABASE_URL`, `DATABASE_USERNAME`,
  `DATABASE_PASSWORD`, `REDIS_URL`) are all supplied by compose, by the Kubernetes ConfigMap + Secret template,
  and by `FoundationIT`; readiness group is `readinessState,db,redis`.
- **NOT RUN:** Flyway itself, the JDBC driver, Spring property binding, Hikari/Lettuce, and the actuator
  readiness group - they require the Spring application. Phase 0 has intentionally no migration, so Flyway
  will find an empty `db/migration` folder; this behaviour is unverified.

### 4. Frontend - PASS
`npm ci` (clean install) PASS; `ng lint` PASS; `ng test` PASS (3 files, **7/7 tests**); `ng build` (production) PASS;
built `index.html` contains no inline script/style (required by the nginx CSP); `npm audit`: 0 vulnerabilities.
nginx config (rendered like the image does) was exercised earlier with `nginx -t` and live requests: SPA fallback,
`/api/` and readiness proxied, other `/actuator/*` not proxied, security headers incl. CSP present.

### 5. ML service - PASS (after a fix)
`ruff check` PASS, `ruff format --check` PASS, `pytest` PASS (**3/3**), real uvicorn boot -> `/health` HTTP 200.
**Finding and fix:** the original pins (`fastapi 0.115.12`, written from memory) pulled `starlette 0.46.2`,
which `pip-audit` showed has about eight known advisories (fixes up to 1.3.1). `requirements.txt` now pins the
full resolved runtime set (fastapi 0.143.0, starlette 1.7.0, uvicorn 0.54.0, pydantic 2.14.0, ...), the dev file
pins pytest 9.1.1 / httpx 0.28.1 / ruff 0.17.0. `pip-audit` on runtime pins, dev pins and the whole installed
environment: **no known vulnerabilities**. No test was modified. One new `StarletteDeprecationWarning`
(test client will require `httpx2` in future) is left as is.

### 6. Docker Compose - PARTIAL
- PASS: `docker compose config` (Compose v5.6.0 standalone binary) validates the file.
- PASS: all three Dockerfiles pass `hadolint` (DL3025 suppressed on HEALTHCHECK lines only, with a reason).
- **NOT RUN:** `docker compose build`, `up`, health-gated startup, `scripts/smoke-test.sh` against a live stack
  (only its syntax was checked with `sh -n`). Reason: no daemon and no registry access.
  Unverified as a result: all three image builds, the `jarmode=tools extract` step in the backend image,
  that the base-image tags exist (`postgres:17-alpine`, `redis:7-alpine`, `nginxinc/nginx-unprivileged:1.27-alpine`,
  `eclipse-temurin:21-jre-alpine`, `maven:3.9-eclipse-temurin-21`, `node:22-alpine`, `python:3.12-slim`),
  and service-to-service networking. The CI job `docker-and-smoke` runs exactly this.

### 7/8. Kubernetes and Terraform - PASS
`kustomize build` PASS; `kubeconform -strict` against Kubernetes 1.31: **10 resources valid, 0 invalid** (9 rendered + the
Secret template); OpenTofu 1.13.1 `fmt -check`, `init -backend=false`, `validate` PASS; `actionlint` on CI PASS.
Not run: `kubectl apply --dry-run` (no cluster/kubectl), `terraform` binary itself (OpenTofu used).
Trivy misconfiguration scan (offline checks; vulnerability DB unreachable):
| ID | Severity | Where | Disposition |
|---|---|---|---|
| DS-0002 root user | HIGH | frontend Dockerfile | **Fixed:** explicit `USER 101` (base image already ran as 101); rescan clean |
| KSV-0014 root FS writable | HIGH | frontend Deployment | **Open, deliberate:** nginx renders its config at start-up. Making it read-only needs emptyDir mounts that I cannot test without running the image |
| KSV-0125 untrusted registry | MEDIUM | all Deployments | **Open:** placeholder image names; set in Phase 11 |
| KSV-0011 no CPU limit | LOW | all | **Open, deliberate** (memory limits set; CPU limits can throttle JVMs) |
| KSV-0020/21 UID/GID <= 10000 | LOW | frontend | **Open:** nginx-unprivileged uses 101 |
Backend and ML pods: no findings beyond the above. docker-compose.yml, backend/ML Dockerfiles, Terraform: no findings.

### 9. Hardcoded credentials and secrets - PASS with notes
- gitleaks 8.30.1: **0 leaks** across all 7 commits (git history) and the working tree.
- Trivy secret scanner: none.
- `application.yml` has **no** default for any credential (fails fast if unset).
- Kubernetes: `secrets.example.yaml` contains only `REPLACE_ME`; `secrets.yaml`, `.env` are git-ignored
  (`git check-ignore` confirmed).
- **Note (by design, not a leak):** `docker-compose.yml` and `.env.example` contain development-only default
  passwords (`cafeflow_dev_only`, `redis_dev_only`) so `docker compose up` works unconfigured; ports are bound to
  127.0.0.1. These values must never be reused outside a developer machine.
- Dependency vulnerabilities: npm 0, Python 0 (after fix). **Java dependencies not audited** (cannot resolve them);
  Trivy's vulnerability database was unreachable, so image/OS CVEs were not scanned. CI's `security-scan` job will.

## Changes made in this pass
1. `ml-service/requirements.txt`, `requirements-dev.txt`: exact, audited pins (fixes known-vulnerable Starlette).
2. `frontend/Dockerfile`: explicit `USER 101`.
3. `.github/workflows/ci.yml`: `pip-audit` step (ml-service) and `npm audit --audit-level=high` step (frontend).
4. This report. **No tests were weakened or removed; no functionality was removed; no Phase 1 work was done.**

## Remaining limitations (blocking Phase 1 sign-off)
1. Backend never compiled or tested (Boot 4.1.1 + Testcontainers 2.x coordinates/APIs unverified).
2. Docker images never built; compose stack never started; smoke test never run against a live stack.
3. Java dependency tree and container-image CVEs never scanned.
4. Open hardening items in the table above (frontend root filesystem, placeholder registry, CPU limits).
5. `trivy-action@0.28.0` pin and the base-image tags should be reviewed against current releases.

## Exit criteria to close Phase 0
CI green on: `backend` (`./mvnw verify` incl. `FoundationIT`), `docker-and-smoke`, `security-scan`, and the
unchanged frontend / ml-service / infrastructure jobs. Then update this report with those results.
