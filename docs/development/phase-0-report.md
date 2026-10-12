# Phase 0 Verification Report

**Overall status: PHASE 0 IS NOT COMPLETE.** Everything that can run in the authoring sandbox passes, but the
Spring Boot backend has **never been built by Maven or tested**, and the Docker stack has **never been built or
started**. Phase 0 closes only when the CI jobs listed under "Exit criteria" are green. Phase 1 has not been started.

Date of verification: 2026-10-10. Legend: **PASS** = executed here and succeeded. **FAIL** = executed and failed.
**BLOCKED** = cannot be executed in this environment (reason given). Nothing is marked PASS unless it ran.

## 1. Corrections to earlier reports
- Report v1 claimed a `javac` syntax check found no errors. **False:** `javac` was not installed and my filter hid the
  failure. Fixed and disclosed in v2.
- During this pass the same class of mistake nearly happened again: a source-level `javac` run printed "0 errors" because
  the `time` command did not exist in this shell, so `javac` never ran. It was caught because no `.class` files existed.
  The probe now checks the exit code, the amount of output, class output, and a **negative control** (section 4).
- v2 pinned FastAPI 0.143.0 and Starlette 1.7.0 because they were the newest. That was imprudent (see section 2).

## 2. Python dependencies - PASS (re-selected)
**Compatibility audit (from the packages' declared metadata and the FastAPI project's own history):**
- FastAPI 0.142.2 declares `starlette>=0.46.0`, `pydantic>=2.9.0`, `typing-extensions>=4.8.0`, `typing-inspection>=0.4.2`,
  `annotated-doc>=0.0.2` and `opentelemetry-api>=1.44.0` - lower bounds only, no upper bounds. An open range only says a version is *allowed*, not that it was *tested*.
- FastAPI's release notes show which Starlette each series was tested with: 0.137.2-0.141.x with Starlette 1.3.1,
  **0.142.0-0.142.2 with 1.6.0**, 0.142.3-0.143.0 with 1.7.0.
- The pair pinned now, **FastAPI 0.142.2 + Starlette 1.6.0**, is therefore the pair FastAPI's CI used. It also contains
  Starlette 1.5.1's `FileResponse` range-handling fixes (not used by this service) and is above every advisory
  `pip-audit` reported for the earlier 0.46.2 pin.
- Rejected: FastAPI 0.143.0 (1 day old; 0.142.3-0.143.0 are OpenTelemetry changes including a breaking change shipped
  within two days) and FastAPI 0.141.1 + Starlette 1.7.0 (allowed by metadata but never tested together).
- `httpx` (last release Dec 2024) is replaced by `httpx2` 2.13.1, which Starlette's test client now asks for and which is
  maintained under the `pydantic` GitHub organisation.
- Transitive packages that floated to releases only 0-8 days old were pinned to older releases that satisfy the declared ranges
  (httptools 0.8.0, uvloop 0.22.1, websockets 17.1, python-dotenv 1.2.3, opentelemetry-api 1.45.0). FastAPI 0.142.2 itself
  is 9 days old, a deliberate exception because compatibility with a tested Starlette pair outranks the 14-day soak rule.

**Exact runtime pins tested** (`ml-service/requirements.txt`; Python 3.12.3, pip 26.2.1):

| Package | Version |
|---|---|
| `annotated-doc` | 0.0.5 |
| `annotated-types` | 0.8.0 |
| `anyio` | 4.15.1 |
| `click` | 8.5.0 |
| `fastapi` | 0.142.2 |
| `h11` | 0.16.0 |
| `httptools` | 0.8.0 |
| `idna` | 3.20 |
| `opentelemetry-api` | 1.45.0 |
| `pydantic` | 2.13.5 |
| `pydantic_core` | 2.46.5 |
| `python-dotenv` | 1.2.3 |
| `PyYAML` | 6.0.3 |
| `starlette` | 1.6.0 |
| `typing-inspection` | 0.4.4 |
| `typing_extensions` | 4.16.0 |
| `uvicorn[standard]` | 0.54.0 |
| `uvloop` | 0.22.1 |
| `watchfiles` | 1.3.0 |
| `websockets` | 17.1 |

Dev tooling (`requirements-dev.txt`): pytest==9.1.1, httpx2==2.13.1, ruff==0.16.9.

**Results (fresh venv, installed only from the committed files):** `pip install` PASS; `pip check` PASS (no broken requirements);
`ruff check` PASS; `ruff format --check` PASS; `pytest` PASS (**6 passed**, 1 warning); real `uvicorn` boot answered
`/health` 200 and `/docs` 404; `pip-audit` on the pinned files PASS; `pip-audit` on the whole installed environment
(transitive packages included) PASS (pip-audit 2.10.1).
**New tests:** `tests/test_dependency_pins.py` (3 tests) fails if the environment drifts from `requirements.txt` or if the
pins violate FastAPI's declared requirements. Negative controls: pinning `starlette==1.7.0` or `pydantic==2.8.0` made it fail.
**Open warning (third-party, not hidden):** `DeprecationWarning: anyio.abc.BlockingPortal alias is deprecated`, raised at import
time by `starlette/testclient.py:53` (Starlette 1.6.0 with AnyIO 4.15.1). It does not affect the running service. I tried
`filterwarnings = error`, saw it fail on this warning, and reverted rather than adding an ignore filter. It disappears with
Starlette 1.7.0; bump FastAPI and Starlette **together** once FastAPI's OpenTelemetry changes settle.

## 3. Frontend - PASS
@angular/core 22.2.1, @angular/cli 22.2.2, typescript 6.0.3, vitest 5.0.3, jsdom 30.1.2, angular-eslint 22.5.0, rxjs 7.8.2; Node v22.23.3. `npm ci` PASS; `ng lint` PASS; `ng test` PASS (**7/7**); `ng build` (production) PASS;
`npm audit --audit-level=high` PASS (0 vulnerabilities). nginx config was exercised with a real nginx (SPA fallback, `/api/` and
readiness proxied, other `/actuator/*` not proxied, CSP and security headers present); not re-run against the final base image.

## 4. Spring Boot backend - NOT BUILT (blocked) / reviewed against upstream source
**Blocked:** Maven Central is unreachable (`./mvnw` cannot download Maven; apt Maven 3.8.7 parses `pom.xml` and then fails with
`Unknown host repo.maven.apache.org`). **No Maven build, no unit test and no integration test has run. I do not claim the backend compiles.**

**Review evidence (cloned and read at the exact release tags):**
| Check | Result |
|---|---|
| Boot `4.1.1` tag exists; managed versions read from its BOM source | Spring Framework **7.0.9**, Spring Security **7.1.1**, Flyway **12.4.0**, Hibernate **7.4.5**, Testcontainers **2.0.5**, JUnit Jupiter **6.0.3**, Jackson **3.1.5**, Tomcat 11.0.24 |
| Every starter in `pom.xml` exists in 4.1.1 | webmvc, validation, security, actuator, data-jpa, flyway, data-redis, test, webmvc-test, security-test: all present |
| `flyway-database-postgresql` managed by the BOM; `maven-failsafe-plugin` configured by `starter-parent` | confirmed |
| All **57** distinct Spring / Testcontainers imports in main and test code | all 57 resolve to real classes in the exact sources (incl. `org.testcontainers.postgresql.PostgreSQLContainer`, `NoResourceFoundException`) |
| Signatures: `AuthenticationEntryPoint.commence`, `AccessDeniedHandler.handle`, `AbstractHttpConfigurer.disable()`, `HttpSecurity` customizer methods, `requestMatchers(String...)`, `SecurityMockMvcRequestPostProcessors.user`, `springSecurity()`, `PostgreSQLContainer(String)` (non-generic), `GenericContainer`, `getMappedPort` | confirmed |
| Behaviour: `DispatcherServlet` 7.0.9 always throws `NoHandlerFoundException` for unmapped paths (so the 404 test expectation and the handler mapping are right); `handlerExceptionResolver` bean name; health ids `db` and `redis`; `local.server.port`; `ecs` structured-log format; property prefixes `spring.data.redis`, `spring.flyway` | confirmed |
| Dockerfile layered-jar step (`-Djarmode=tools ... extract --layers --destination`, four layer directories) | identical to the official Boot 4.1.1 documentation example |
| **`javac` 21 on all 25 of my files with the real Framework, Security, Boot and Testcontainers sources on `-sourcepath`** (full attribution) | **0 errors reported in my files.** About 2,600 errors appear inside upstream sources (optional modules such as OAuth2 are not on the path); none are in `backend/src` |
| Negative control for the probe | 3 deliberately injected mistakes (a typo'd `HttpSecurity` method, `BindingResult` method and Testcontainers method reference) were all detected |

**Limits of that evidence:** it is a type-check against sources, not a Maven build; support libraries were older than the real ones
(servlet 6.0, JUnit 5.10, AssertJ 3.14, SLF4J 1.7); nothing was executed, so Spring wiring, Jackson 3 serialization of `ApiError`,
Security in a plain Spring context, Flyway on an empty migration folder, Hibernate with zero entities, Lettuce and Testcontainers at
runtime are all **untested**.

## 5. PostgreSQL / Redis configuration - PASS (partial)
Real servers (PostgreSQL 16.15 and Redis 7.0.15 from apt; compose uses PostgreSQL 17 and Redis 7.4.11): healthcheck commands,
password authentication (accepted / rejected), DDL rights in `public`, `redis://:<pw>@host:port` URL, persistence off.
**Not executed:** Flyway, the JDBC driver, Spring property binding, actuator readiness.

## 6. Docker Compose and images
- PASS: `docker compose config` (Docker Compose version v5.6.0); `hadolint` on all three Dockerfiles (Haskell Dockerfile Linter 2.15.1).
- **BLOCKED:** `docker compose build/up`, the health-gated start, `scripts/smoke-test.sh` against a live stack, and image
  scanning. Reason: no Docker daemon, and docker.io / ghcr.io / quay.io / mcr.microsoft.com are unreachable.
- **Base-image tags checked against `docker-library/official-images` and the nginx-unprivileged workflows:**
  `postgres:17-alpine`, `redis:7-alpine` (= 7.4.11), `node:22-alpine`, `python:3.12-slim`, `eclipse-temurin:21-jre-alpine` exist.
  **Two defects were found and fixed:** `maven:3.9-eclipse-temurin-21` is no longer published (official image is now Maven 3.10,
  tag `3-eclipse-temurin-21`); `nginx-unprivileged:1.27-alpine` is an abandoned series (only the current stable and mainline lines
  are rebuilt), now `stable-alpine`. The Maven wrapper's 3.9.16 does exist (Apache tag `maven-3.9.16`); the image builds with Maven 3.10, a minor difference.

## 7. CI workflow - reviewed, not yet executed
Confirmed from the workflow: `backend` runs `./mvnw -B test` then `./mvnw -B verify` (unit tests, then Testcontainers ITs and
packaging); `docker-and-smoke` runs `docker compose up -d --build --wait`, which **builds all three images** (backend, frontend,
ml-service), starts the stack, then runs `scripts/smoke-test.sh`; it now also checks that all three images exist and **Trivy-scans each**
(CRITICAL/HIGH, fail on findings). The `infrastructure` job validates configuration only, by design. `actionlint`: PASS.
**Defect found and fixed:** `aquasecurity/trivy-action@0.28.0` does not exist (tags are `v0.x.y`), so `security-scan` would have failed
immediately. It is now pinned to the commit of `v0.36.0` (`ed142fd...`), with its inputs checked against that version's `action.yaml`.
Other actions exist at the pinned majors (checkout v4, setup-java v4, setup-node v4, setup-python v5, setup-terraform v3) but newer
majors exist; they were not bumped because they cannot be tested here.

## 8. Kubernetes, Terraform, secrets - PASS
`kustomize build` (v5.8.3) PASS; `kubeconform -strict` (v0.8.0, Kubernetes 1.31): 10 resources valid, 0 invalid; OpenTofu
v1.13.1 `fmt -check`, `init -backend=false`, `validate` PASS; gitleaks 8.30.1: no leaks in history or working tree.
Trivy 0.75.0 misconfiguration scan, CRITICAL/HIGH: one finding, **KSV-0014 (frontend pod root filesystem writable)**.
Upstream source shows why it is not a one-line fix: if `/etc/nginx/conf.d` is not writable the entrypoint only logs an error and nginx starts
**without the proxy config**. It is recorded in `.trivyignore.yaml` with a statement and an **expiry of 2026-12-31**; the file hides only
that finding on that path (verified).

## 9. Failed checks
No executed check is currently failing. Defects found by this review and fixed: vulnerable and unsuitable Python pins, unmaintained test
client dependency, unpublished Maven image tag, abandoned nginx series, nonexistent Trivy action version. The only command that failed when
run was `./mvnw -B test`, and it failed because the environment cannot reach Maven Central, not because of project code.

## 10. Checks blocked by the environment
Maven build and tests (including `FoundationIT`); Docker image builds; `docker compose up` and `scripts/smoke-test.sh`; Trivy vulnerability
database and image scans; Java dependency vulnerability audit; `kubectl` dry-run; the CI workflow itself.

## 11. Remaining risks
1. Backend unbuilt and untested; runtime behaviour of Boot 4.1.1 wiring is unverified (section 4).
2. Images never built; `stable-alpine` and `maven:3-eclipse-temurin-21` float, so builds are not bit-reproducible.
3. KSV-0014 open until 2026-12-31; LOW/MEDIUM Trivy items remain (placeholder registry, no CPU limits, nginx UID 101).
4. CI image and filesystem scans may fail on newly published CVEs in base images; that is the gate working.
5. GitHub Action majors are not the latest; Starlette test-client deprecation warning; FastAPI 0.142.2 is 9 days old.
6. The Python pins were chosen from evidence at one point in time; FastAPI and Starlette move quickly and must be re-audited on every bump.

## 12. Exact commands for local verification
Run from the repository root on a machine with internet access and Docker.
```bash
# Backend (Java 21, Docker running for the integration tests)
cd backend && ./mvnw -B test              # unit tests: SecurityBaselineTest, GlobalExceptionHandlerTest, RequestIdFilterTest
cd backend && ./mvnw -B verify            # + FoundationIT (Testcontainers PostgreSQL 17 + Redis 7) and packaging
cd backend && ./mvnw -B dependency:tree   # record the exact resolved Java versions, then paste them into this report

# Frontend (Node >= 22.22.3)
cd frontend && npm ci && npm run lint && npm test -- --watch=false && npm run build && npm audit --audit-level=high

# ML service (Python 3.12)
cd ml-service && python3 -m venv .venv && . .venv/bin/activate \
  && pip install -r requirements-dev.txt && pip check && ruff check . && ruff format --check . && pytest \
  && pip install pip-audit && pip-audit -r requirements.txt --disable-pip --no-deps

# Whole stack
docker compose config --quiet && docker compose up -d --build --wait --wait-timeout 300 && scripts/smoke-test.sh
for i in backend frontend ml-service; do trivy image --severity CRITICAL,HIGH --ignore-unfixed --exit-code 1 cafeflow-$i; done
docker compose down -v

# Infrastructure and repository hygiene
kubectl kustomize infrastructure/kubernetes > /tmp/rendered.yaml && kubeconform -strict -kubernetes-version 1.31.0 -summary /tmp/rendered.yaml
(cd infrastructure/terraform && terraform fmt -check -recursive && terraform init -backend=false && terraform validate)
actionlint .github/workflows/ci.yml && hadolint --config .hadolint.yaml backend/Dockerfile frontend/Dockerfile ml-service/Dockerfile
trivy fs --scanners vuln,secret,misconfig --severity CRITICAL,HIGH --ignore-unfixed --ignorefile .trivyignore.yaml .
gitleaks git . && gitleaks dir .
```

## 13. Tools used in this environment
javac 21.0.12.1 (JDK only, no Maven), Node v22.23.3, Docker Compose version v5.6.0, kustomize v5.8.3, kubeconform v0.8.0, OpenTofu v1.13.1,
actionlint 1.7.12, Haskell Dockerfile Linter 2.15.1, gitleaks 8.30.1, Version: 0.75.0, pip-audit 2.10.1.

## 14. Exit criteria to close Phase 0
CI green on `backend` (`./mvnw verify`, including `FoundationIT`), `docker-and-smoke` (build, start, smoke test, image scans) and `security-scan`,
plus the unchanged `frontend`, `ml-service` and `infrastructure` jobs. Then replace sections 4, 6, 7 and 10 with the real results.
