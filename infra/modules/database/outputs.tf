output "cluster_endpoint" {
  value = aws_rds_cluster.main.endpoint
}

output "cluster_reader_endpoint" {
  value = aws_rds_cluster.main.reader_endpoint
}

output "secret_arn" {
  value = aws_secretsmanager_secret.db_credentials.arn
}

output "security_group_id" {
  value = aws_security_group.db.id
}