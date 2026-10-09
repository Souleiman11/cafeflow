export interface Environment {
  /** Base URL of the versioned REST API. Same-origin; never a hardcoded host. */
  apiBaseUrl: string;
  /** Base URL of the backend health endpoints (liveness/readiness only). */
  healthBaseUrl: string;
}

export const environment: Environment = {
  apiBaseUrl: '/api/v1',
  healthBaseUrl: '/actuator/health',
};
