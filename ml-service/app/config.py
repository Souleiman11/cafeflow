"""Runtime configuration, sourced exclusively from environment variables."""

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    service_name: str
    service_version: str
    environment: str


def get_settings() -> Settings:
    return Settings(
        service_name="cafeflow-ml-service",
        service_version=os.getenv("SERVICE_VERSION", "0.1.0"),
        environment=os.getenv("APP_ENV", "development"),
    )
