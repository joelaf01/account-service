terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "3.0.1"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "3.2.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "4.2.1"
    }
    http = {
      source  = "hashicorp/http"
      version = "3.6.0"
    }
  }
}

resource "aws_ecr_repository" "app" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"
  force_delete         = true # This is a demo project meant to be ully torn down between sessions. Setting this to allow clean terraform destroy.

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_iam_role" "cluster" {
  name = "${var.project_name}-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_eks_cluster" "main" {
  name     = "${var.project_name}-cluster"
  role_arn = aws_iam_role.cluster.arn
  version  = "1.36"

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = true
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  depends_on = [aws_iam_role_policy_attachment.cluster_policy]
}

resource "aws_iam_role" "fargate_pod_execution" {
  name = "${var.project_name}-eks-fargate-pod-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "eks-fargate-pods.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "fargate_pod_execution_policy" {
  role       = aws_iam_role.fargate_pod_execution.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSFargatePodExecutionRolePolicy"
}

resource "aws_eks_fargate_profile" "app" {
  cluster_name           = aws_eks_cluster.main.name
  fargate_profile_name   = "${var.project_name}-app"
  pod_execution_role_arn = aws_iam_role.fargate_pod_execution.arn
  subnet_ids             = var.private_subnet_ids

  selector {
    namespace = "default"
  }
}

resource "aws_eks_fargate_profile" "kube_system" {
  cluster_name           = aws_eks_cluster.main.name
  fargate_profile_name   = "${var.project_name}-kube-system"
  pod_execution_role_arn = aws_iam_role.fargate_pod_execution.arn
  subnet_ids             = var.private_subnet_ids

  selector {
    namespace = "kube-system"
  }
}

data "tls_certificate" "eks" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
}

resource "aws_iam_role" "load_balancer_controller" {
  name = "${var.project_name}-eks-lb-controller"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.eks.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
          "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "kubernetes_service_account_v1" "load_balancer_controller" {
  metadata {
    name      = "aws-load-balancer-controller"
    namespace = "kube-system"
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.load_balancer_controller.arn
    }
  }
}

resource "helm_release" "aws_load_balancer_controller" {
  name            = "aws-load-balancer-controller"
  repository      = "https://aws.github.io/eks-charts"
  chart           = "aws-load-balancer-controller"
  namespace       = "kube-system"
  version         = "3.4.2"
  atomic          = true
  cleanup_on_fail = true

  set = [
    { name = "clusterName", value = aws_eks_cluster.main.name },
    { name = "serviceAccount.create", value = "false" },
    { name = "serviceAccount.name", value = kubernetes_service_account_v1.load_balancer_controller.metadata[0].name },
    { name = "vpcId", value = var.vpc_id }
  ]

  depends_on = [aws_eks_fargate_profile.kube_system]
}

data "http" "lb_controller_iam_policy" {
  url = "https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.14.1/docs/install/iam_policy.json"
}

resource "aws_iam_policy" "load_balancer_controller" {
  name   = "${var.project_name}-eks-lb-controller-policy"
  policy = data.http.lb_controller_iam_policy.response_body
}

resource "aws_iam_role_policy_attachment" "load_balancer_controller" {
  role       = aws_iam_role.load_balancer_controller.name
  policy_arn = aws_iam_policy.load_balancer_controller.arn
}

resource "aws_iam_role" "app_pod" {
  name = "${var.project_name}-eks-app-pod-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.eks.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:default:account-service"
          "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "app_pod_secrets_access" {
  name = "${var.project_name}-app-pod-secrets-access"
  role = aws_iam_role.app_pod.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "secretsmanager:GetSecretValue"
      Resource = [var.db_secret_arn]
    }]
  })
}

resource "aws_iam_role_policy" "app_pod_dynamodb_access" {
  name = "${var.project_name}-app-pod-dynamodb-access"
  role = aws_iam_role.app_pod.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
      Resource = [var.dirty_flag_table_arn]
    }]
  })
}

resource "aws_eks_access_entry" "console_admin" {
  count         = var.console_admin_principal_arn != "" ? 1 : 0
  cluster_name  = aws_eks_cluster.main.name
  principal_arn = var.console_admin_principal_arn
}

resource "aws_eks_access_policy_association" "console_admin" {
  count         = var.console_admin_principal_arn != "" ? 1 : 0
  cluster_name  = aws_eks_cluster.main.name
  principal_arn = aws_eks_access_entry.console_admin[0].principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}

resource "kubernetes_namespace_v1" "aws_observability" {
  metadata {
    name = "aws-observability"
    labels = {
      "aws-observability" = "enabled"
    }
  }
}

resource "kubernetes_config_map_v1" "aws_logging" {
  metadata {
    name      = "aws-logging"
    namespace = kubernetes_namespace_v1.aws_observability.metadata[0].name
  }

  data = {
    "filters.conf" = <<-EOT
      [FILTER]
          Name kubernetes
          Match kube.*
          Merge_Log On
          Keep_Log Off
    EOT

    "output.conf" = <<-EOT
      [OUTPUT]
          Name cloudwatch_logs
          Match *
          region us-east-1
          log_group_name ${aws_cloudwatch_log_group.app.name}
          log_stream_prefix fargate-
          auto_create_group false
    EOT
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/eks/${var.project_name}"
  retention_in_days = 7
}

resource "aws_iam_role_policy" "fargate_logging" {
  name = "${var.project_name}-fargate-logging"
  role = aws_iam_role.fargate_pod_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:CreateLogGroup",
        "logs:DescribeLogStreams"
      ]
      Resource = ["${aws_cloudwatch_log_group.app.arn}:*"]
    }]
  })
}
