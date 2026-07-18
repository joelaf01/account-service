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
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "3.0.1"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "3.2.0"
    }
  }

  backend "s3" {
    key          = "main/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true
    encrypt      = true
  }
}

provider "aws" {
  region  = var.aws_region
}

data "aws_eks_cluster_auth" "main" {
  name = module.eks.cluster_name
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
  token                  = data.aws_eks_cluster_auth.main.token
}

provider "helm" {
  kubernetes = {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
    token                  = data.aws_eks_cluster_auth.main.token
  }
}

module "cache" {
  source = "./modules/cache"

  project_name               = var.project_name
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
}

module "database" {
  source = "./modules/database"

  project_name               = var.project_name
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  database_name              = var.database_name
}

module "dirty_flag" {
  source       = "./modules/dirty-flag"

  project_name = var.project_name
}

module "eks" {
  source = "./modules/eks"

  project_name       = var.project_name
  private_subnet_ids = module.vpc.private_subnet_ids

  db_endpoint = module.database.cluster_endpoint
  db_name = var.database_name
  db_secret_arn = module.database.secret_arn

  cache_endpoint = module.cache.endpoint
  cache_port = module.cache.port

  dirty_flag_table_arn = module.dirty_flag.table_arn
  dirty_flag_table_name = module.dirty_flag.table_name
}

module "vpc" {
  source = "./modules/vpc"

  project_name         = var.project_name
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}

resource "aws_security_group_rule" "db_from_eks" {
  type = "ingress"
  from_port = 5432
  to_port = 5432
  protocol = "tcp"
  security_group_id = module.database.security_group_id
  source_security_group_id = module.eks.cluster_security_group_id
}

resource "aws_security_group_rule" "cache_from_eks" {
  type = "ingress"
  from_port = 6379
  to_port = 6379
  protocol = "tcp"
  security_group_id = module.cache.security_group_id
  source_security_group_id = module.eks.cluster_security_group_id
}