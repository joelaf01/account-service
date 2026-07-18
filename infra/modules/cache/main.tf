resource "aws_security_group" "cache" {
  name        = "${var.project_name}-cache-sg"
  description = "Allow Redis access from application tier"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.project_name}-cache-sg"
  }
}

resource "aws_elasticache_serverless_cache" "main" {
  engine               = "redis"
  name                 = "${var.project_name}-cache"
  major_engine_version = "7"
  subnet_ids           = var.private_subnet_ids
  security_group_ids   = [aws_security_group.cache.id]

  cache_usage_limits {
    data_storage {
      maximum = 1
      unit    = "GB"
    }
    ecpu_per_second {
      maximum = 1000
    }
  }
}
