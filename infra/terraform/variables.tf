variable "aws_region" {
  description = "AWS region to deploy infrastructure"
  type        = string
  default     = "ap-southeast-1"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.10.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["ap-southeast-1a", "ap-southeast-1b", "ap-southeast-1c"]
}

variable "public_subnets" {
  description = "Public subnet CIDRs"
  type        = list(string)
  default     = ["10.10.0.0/24", "10.10.1.0/24", "10.10.2.0/24"]
}

variable "private_subnets" {
  description = "Private subnet CIDRs"
  type        = list(string)
  default     = ["10.10.10.0/24", "10.10.11.0/24", "10.10.12.0/24"]
}

variable "db_master_username" {
  description = "Master username for Oracle RDS"
  type        = string
}

variable "db_master_password" {
  description = "Master password for Oracle RDS"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Deployment environment (dev/stg/prod)"
  type        = string
  default     = "lab"
}

variable "owner" {
  description = "Owner tag"
  type        = string
  default     = "corebank-team"
}
