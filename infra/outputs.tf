output "vpc_id" {
  value = module.vpc.vpc_id
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.vpc.private_subnet_ids
}

output "cluster_endpoint" {
  value = module.database.cluster_endpoint
}

output "cluster_reader_endpoint" {
  value = module.database.cluster_reader_endpoint
}

output "secret_arn" {
  value = module.database.secret_arn
}

output "endpoint" {
  value = module.cache.endpoint
}

output "port" {
  value = module.cache.port
}

output "ecr_repository_url" {
  value = module.eks.ecr_repository_url
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "app_pod_role_arn" {
  value = module.eks.app_pod_role_arn
}

output "dirty_flag_table_name" {
  value = module.dirty_flag.table_name
}

output "dirty_flag_table_arn" {
  value = module.dirty_flag.table_arn
}