# Docker

Each service owns its Dockerfile next to its source:

- `backend/Dockerfile` - Spring Boot (layered, non-root, JRE alpine)
- `frontend/Dockerfile` - Angular build served by non-root nginx
- `ml-service/Dockerfile` - FastAPI on python-slim, non-root

The local stack is defined in the repository-root `docker-compose.yml`.
