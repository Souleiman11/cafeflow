# CaféFlow

Multi-tenant café operations SaaS: customers order by QR, waiters and baristas run service in
real time, owners and managers get analytics, AI explanations and forecasts.

| Document | Purpose |
|---|---|
| [`MASTER_SPEC.md`](MASTER_SPEC.md) | WHAT the product and architecture must be (source of truth) |
| [`AI_CODING_AGENT_EXECUTION_SPEC.md`](AI_CODING_AGENT_EXECUTION_SPEC.md) | HOW it must be implemented (phases, rules, definition of done) |
| [`docs/`](docs) | Architecture, API, database, security, deployment, ADRs |

**Current status: Phase 0 (Foundation) - no business features yet.** See
[`docs/development/phase-0-report.md`](docs/development/phase-0-report.md).

## Architecture

```
Angular (nginx)  ->  Spring Boot modular monolith  ->  PostgreSQL (source of truth)
                                   |                ->  Redis (cache / rate limiting only)
                                   +-> Python/FastAPI ML service (separate)
```

| Directory | Contents |
|---|---|
| `backend/` | Java 21, Spring Boot 4.1, Maven, Flyway, Actuator |
| `frontend/` | Angular 22 (standalone components, Vitest, ESLint) |
| `ml-service/` | Python 3.12, FastAPI, pytest, ruff |
| `infrastructure/` | `kubernetes/` (kustomize), `terraform/`, `docker/` notes |
| `.github/workflows/` | CI pipeline |
| `scripts/` | `smoke-test.sh` for the local stack |

## Run everything locally

Requires Docker with Compose v2.

```bash
docker compose up --build --wait     # first build takes a few minutes
scripts/smoke-test.sh                # verifies every service and service-to-service paths
```

| Service | URL |
|---|---|
| Frontend | http://127.0.0.1:4200 |
| Backend health | http://127.0.0.1:8080/actuator/health/readiness |
| ML service | http://127.0.0.1:8000/health |

Defaults are **development-only**; override through `.env` (see `.env.example`).

## Run the tests

```bash
cd backend     && ./mvnw test            # unit tests (no Docker)
cd backend     && ./mvnw verify          # + Testcontainers integration tests (needs Docker)
cd frontend    && npm ci && npm run lint && npm test -- --watch=false && npm run build
cd ml-service  && pip install -r requirements-dev.txt && ruff check . && pytest
```

## Ground rules (summary)

Tenant isolation and backend authorization are mandatory; the database is the source of truth;
AI interprets verified metrics and never calculates them; ML (not an LLM) produces forecasts;
every schema change is a Flyway migration. Full list: the two specification documents.
