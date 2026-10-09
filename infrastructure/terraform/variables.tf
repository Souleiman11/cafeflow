variable "project" {
  description = "Project name used as a prefix for all resources."
  type        = string
  default     = "cafeflow"
}

variable "environment" {
  description = "Deployment environment."
  type        = string

  validation {
    condition     = contains(["development", "staging", "production"], var.environment)
    error_message = "environment must be one of: development, staging, production."
  }
}
