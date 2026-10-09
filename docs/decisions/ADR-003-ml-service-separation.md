# ADR-003: Separate ML service

**Status:** Accepted. **Source:** MASTER_SPEC sections 91-96.

Forecasting runs in a Python/FastAPI service called by the backend over an internal API. Spring Boot
keeps authorization, tenant scoping and orchestration; ML does data preparation, training and
prediction. With insufficient data the service returns an explicit "insufficient data" result.
LLMs are never used for numerical forecasting. Phase 0 exposes only `/health`.
