output "name_prefix" {
  description = "Resource name prefix for this environment."
  value       = local.name_prefix
}

output "common_tags" {
  description = "Tags/labels applied to every resource."
  value       = local.common_tags
}
