"""Health endpoints used by Docker, Kubernetes probes and the Spring Boot backend."""

from fastapi import APIRouter
from pydantic import BaseModel

from app.config import get_settings

router = APIRouter(tags=["health"])


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Liveness/readiness. Phase 0 has no external dependencies (no model is loaded yet)."""
    settings = get_settings()
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
    )
