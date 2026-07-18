variable "project_name" {
  description = "Name prefix used for tagging and naming database resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID the database security group belongs to"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "engine_version" {
  description = "Aurora PostgreSql engine version"
  type        = string
  default     = "18"
}

variable "database_name" {
  description = "Initial database name"
  type        = string
  default     = "mydatabase"
}

variable "master_username" {
  description = "Master username for the Aurora cluster"
  type        = string
  default     = "myuser"
}

variable "min_capacity" {
  description = "Minimum Aurora Serverless v2 capacity in ACUs"
  type        = number
  default     = 0.5
}

variable "max_capacity" {
  description = "Maximum Aurora Serverless v2 capacity in ACUs"
  type        = number
  default     = 1
}
