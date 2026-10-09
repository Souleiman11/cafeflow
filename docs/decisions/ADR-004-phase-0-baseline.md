# ADR-004: Phase 0 baseline decisions

**Status:** Accepted. Technical decisions made without changing any business rule.

1. **Spring Boot 4.1.1, not 3.5.** Boot 3.5 left OSS support on 2026-06-30; starting a production
   system on it would mean no free security patches. Boot 4 is modular: starters are
   `spring-boot-starter-webmvc`, `-flyway`, and test starters such as `-webmvc-test`; Testcontainers is 2.x.
2. **Deny-by-default security from day one.** `anyRequest().denyAll()` except health/info.
3. **No Flyway migration in Phase 0**, so `V1__create_tenants.sql` stays V1 per the spec's sequence.
4. **Unit vs integration tests.** `*Test` (surefire, no Docker) vs `*IT` (failsafe, Testcontainers).
   Tests avoid Boot 4's relocated slice annotations where plain Spring test APIs suffice.
5. **springdoc deferred to Phase 1** (no endpoints yet; version must match Boot 4).
6. **WebSocket/STOMP dependency deferred to Phase 7** (avoid speculative dependencies).
7. **Frontend nginx proxies `/api/` and `/actuator/health/` only**, keeping a single origin, no CORS
   config and the rest of `/actuator` unreachable. Angular critical-CSS inlining is disabled so a strict
   CSP (no inline script) works.
8. **`secrets.yaml`:** the spec lists it in the Kubernetes tree (section 153) but also forbids
   committing it (section 43). Resolved with a committed `secrets.example.yaml` and a git-ignored `secrets.yaml`.
9. **Frontend folders** use the union of the two specs (`barista` from the execution spec,
   `onboarding` from the master spec); `kitchen` is not created.
10. **Structured JSON logging** (ECS) enabled by default; MDC carries `requestId`
    (tenant/user ids are added in Phase 1).
