terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.36.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.8.1"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = "terraform"
}

module "cache" {
  source = "./modules/cache"

  project_name               = var.project_name
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.ecs.security_group_id]
}

module "database" {
  source = "./modules/database"

  project_name               = var.project_name
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.ecs.security_group_id]
  database_name = var.database_name
}

module "ecs" {
  source = "./modules/ecs"

  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids

  db_endpoint = module.database.cluster_endpoint
  db_name = var.database_name
  db_secret_arn = module.database.secret_arn

  cache_endpoint = module.cache.endpoint
  cache_port = module.cache.port
}

module "vpc" {
  source = "./modules/vpc"

  project_name         = var.project_name
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}
