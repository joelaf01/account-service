resource "aws_dynamodb_table" "dirty_flag" {
  name = "${var.project_name}-dirty-flag"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "accountId"

  attribute {
    name = "accountId"
    type = "S"
  }

  ttl {
    attribute_name = "ttl"
    enabled = true
  }

  tags = {
    Name = "${var.project_name}-dirty-flag"
  }
}
