variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "ap-southeast-2" # Sydney
}

variable "project" {
  description = "Prefix for resource names"
  type        = string
  default     = "splitpay"
}

variable "log_retention_days" {
  description = "How long to keep Lambda logs"
  type        = number
  default     = 1
}
