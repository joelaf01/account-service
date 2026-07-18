variable "project_name" {
  description = "Name prefix used for tagging and naming cache resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID the cache security group belongs to"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for the cache subnet group"
  type        = list(string)
}
