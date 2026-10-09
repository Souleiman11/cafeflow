# Terraform

Cloud-agnostic infrastructure skeleton (Phase 0). No provider or remote state is configured yet;
that arrives in Phase 11 together with the chosen cloud.

```bash
terraform init -backend=false
terraform validate
terraform plan -var-file=environments/development.tfvars
```

Kubernetes application manifests live in `../kubernetes` and are deliberately separate.
