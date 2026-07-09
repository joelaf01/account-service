variable "project_name" {
  description = "Name prefix used for tagging and naming VPC resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID the ECS security group belongs to"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs the ECS service will run in"
  type        = list(string)
}

variable "image_tag" {
  description = "Tag of the container image to deploy from ECR"
  type        = string
  default     = "latest"
}

variable "db_endpoint" {
  description = "Aurora cluster endpoint"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_secret_arn" {
  description = "ARN of the Secrets Manager secret holding DB credentials"
  type        = string
}

variable "cache_endpoint" {
  description = "ElastiCache Serverless endpoint address"
  type        = string
}

variable "cache_port" {
  description = "ElastiCache Serverless endpoint port"
  type        = number
}
