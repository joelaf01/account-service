output "table_name" {
  value = aws_dynamodb_table.dirty_flag.name
}

output "table_arn" {
  value = aws_dynamodb_table.dirty_flag.arn
}