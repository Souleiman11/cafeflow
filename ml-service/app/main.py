"""CaféFlow ML service entrypoint.

Phase 0 foundation only: this service exposes health endpoints. Forecasting
(Phase 10) is deliberately NOT implemented. When it is, it must return an
explicit "insufficient data" result instead of fabricating predictions
(MASTER_SPEC sections 91-96, 139).
"""

from fastapi import FastAPI

from app.api import health
from app.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    application = FastAPI(
        title="CaféFlow ML Service",
        version=settings.service_version,
        # Internal service: interactive docs are not exposed.
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    application.include_router(health.router)
    return application


app = create_app()
