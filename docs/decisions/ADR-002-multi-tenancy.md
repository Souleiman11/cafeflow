# ADR-002: Multi-tenancy principles

**Status:** Accepted (principles); enforcement mechanism decided in Phase 1.

Shared database, shared schema, `tenant_id` on every tenant-owned table. Tenant context is derived
from the authenticated principal's membership or the validated customer QR session - never from a
client-supplied `tenantId`. Every tenant-owned query is tenant-scoped; cross-tenant access
returns 403/404 even when the caller knows the UUID. Cross-tenant tests are mandatory.
