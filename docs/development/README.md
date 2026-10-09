# Development

Prerequisites: JDK 21, Node 22.22.3+ (Angular 22 CLI requirement), Python 3.12, Docker.

Run services individually against `docker compose up postgres redis`:

```bash
# backend (env vars have no defaults on purpose)
export DATABASE_URL=jdbc:postgresql://localhost:5432/cafeflow DATABASE_USERNAME=cafeflow \
       DATABASE_PASSWORD=cafeflow_dev_only REDIS_URL=redis://:redis_dev_only@localhost:6379
cd backend && ./mvnw spring-boot:run

cd frontend && npm ci && npm start         # proxies /api and /actuator/health to localhost:8080
cd ml-service && uvicorn app.main:app --reload
```

Tests: `./mvnw test` (unit), `./mvnw verify` (+ Testcontainers ITs, needs Docker).
Workflow per feature: READ -> PLAN -> IMPLEMENT -> COMPILE -> TEST -> VERIFY (execution spec section 9).
