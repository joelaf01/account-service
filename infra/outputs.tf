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
