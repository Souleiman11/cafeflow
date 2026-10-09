# ADR-001: Modular monolith

**Status:** Accepted. **Source:** MASTER_SPEC sections 59-60, execution spec section 6.

One Spring Boot deployable with module packages (`auth`, `order`, ...). Modules communicate through
services and domain events, not HTTP. Kubernetes does not imply microservices. The ML service is
the only separate service because it has a different runtime (Python) and lifecycle.
