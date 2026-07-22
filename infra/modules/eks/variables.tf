variable "project_name" {
  description = "Name prefix used for tagging and naming EKS resources"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs the EKS cluster and Fargate profiles run in"
  type        = list(string)
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

variable "dirty_flag_table_name" {
  description = "Name of the dirty flag DynamoDB table"
  type        = string
}

variable "dirty_flag_table_arn" {
  description = "ARN of the dirty flag DynamoDB table"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID the EKS cluster runs in"
  type = string
}