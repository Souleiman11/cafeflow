# Architecture overview

```
                +-----------------+
                |  Angular (nginx)|   /api, /actuator/health -> backend
                +--------+--------+
                         |
                +--------v--------+        +------------------+
                |   Spring Boot   |------->|  ML service      |  (FastAPI, forecasting - Phase 10)
                | modular monolith|        +------------------+
                +----+-------+----+
                     |       |
              PostgreSQL    Redis
           (source of truth) (cache / rate limit / temp state)
```

## Backend modules (`com.cafeflow`)

`auth`, `tenant`, `user`, `table`, `session`, `menu`, `order`, `payment`, `notification`,
`analytics`, `ai`, `audit`, plus `common` (security baseline, error handling, request
correlation). Each module will contain `controller / service / repository / entity / dto /
mapper / exception`. Module packages exist from Phase 0 as documented boundaries; code arrives
in the phase that owns it.

## Core invariants (from MASTER_SPEC)

- `Order -> TableSession -> Table` (never `Order -> Table`); a session may span several tables.
- Tenant context is derived from the authenticated principal / QR session, never from client input.
- Backend authorization is authoritative; frontend guards are UX only.
- PostgreSQL is the source of truth; Redis, WebSocket, AI, ML and the frontend never are.

## Phase 0 runtime behaviour

The backend exposes only `/actuator/health/**` and `/actuator/info`. Everything else is
**denied by default** (401 anonymous / 403 authenticated) until a later phase opens it.
Readiness requires PostgreSQL and Redis; liveness does not.
