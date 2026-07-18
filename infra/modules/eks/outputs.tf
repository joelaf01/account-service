output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

output "cluster_ca_certificate" {
  value = aws_eks_cluster.main.certificate_authority[0].data
}

output "cluster_security_group_id" {
  value = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "app_pod_role_arn" {
  value = aws_iam_role.app_pod.arn
}

output "db_endpoint" {
  value = var.db_endpoint
}

output "db_name" {
  value = var.db_name
}

output "db_secret_arn" {
  value = var.db_secret_arn
}

output "cache_endpoint" {
  value = var.cache_endpoint
}

output "cache_port" {
  value = var.cache_port
}

output "dirty_flag_table_name" {
  value = var.dirty_flag_table_name
}