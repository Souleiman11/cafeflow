locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = {
    project     = var.project
    environment = var.environment
    managed_by  = "terraform"
  }
}

# Phase 11 adds (kept separate from the Kubernetes application manifests):
#   - Kubernetes cluster
#   - Managed PostgreSQL
#   - Managed Redis
#   - Container registry
#   - Object storage
#   - DNS / TLS / networking
