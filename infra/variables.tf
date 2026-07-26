variable "project_name" {
  description = "Name prefix used for tagging and naming VPC resources"
  type        = string
}

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of AZs to spread subnets across"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets, one per AZ"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets, one per AZ"
  type        = list(string)
}

variable "database_name" {
  description = "Name of the Aurora database"
  type        = string
  default     = "mydatabase"
}

variable "console_admin_principal_arn" {
  description = "IAM principal ARN to grant EKS cluster-admin access for local/console debugging. Leave empty to skip."
  type        = string
  default     = ""
}