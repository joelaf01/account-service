variable "project_name" {
  description = "Name prefix used for the state bucket and lock table"
  type        = string
}

variable "aws_region" {
  description = "AWS region for the state backend resources"
  type        = string
}

variable "github_org" {
  description = "GitHub organization or username that owns the repo"
  type        = string
}

variable "github_repo" {
  description = "Github repository name"
  type        = string
}

variable "terraform_aws_cli_profile" {
  description = "AWS CLI profile terraform will run as"
  type = string
}