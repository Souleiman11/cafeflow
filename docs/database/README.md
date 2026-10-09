# Database

PostgreSQL is the source of truth. Flyway owns the schema (`backend/src/main/resources/db/migration`).

- Hibernate runs with `ddl-auto: validate`; it never alters the schema.
- Never edit an applied migration; add `V<n>__description.sql`.
- Migration numbering follows MASTER_SPEC section 154 (`V1__create_tenants.sql` ... ).
  **Phase 0 deliberately contains no migration** so that V1 remains `create_tenants`.
- Tenant-owned tables carry `tenant_id` NOT NULL with a foreign key and an index.
