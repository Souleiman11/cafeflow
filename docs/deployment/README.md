# Deployment

- **Local:** `docker compose up --build --wait`, then `scripts/smoke-test.sh`.
- **Kubernetes:** `kubectl apply -k infrastructure/kubernetes` after creating the `cafeflow-secrets`
  Secret out-of-band (see `secrets.example.yaml`). Images use placeholder names/tags; set the real
  registry in an environment overlay with kustomize `images:`.
- PostgreSQL and Redis are **managed services**, not deployed in the cluster (MASTER_SPEC section 110).
- **Terraform:** skeleton only (`infrastructure/terraform`); providers and state backend arrive in Phase 11.
- Rollback strategy, backups, monitoring and TLS are Phase 11 deliverables.
