# Purpose

This project is a demo application exploring AWS resources by implementing a resilient REST API. This REST API employs
a cache-aside lookup with dirty flag invalidation, wrapped by circuit breakers with bidirectional fallback.

# Architecture

## Compute

The application is a Java 25 Spring Boot 4 REST API deployed as a Docker container into Amazon EKS Fargate

## Data Tier

The data tier consists of three components. Amazon Aurora Serverless (Postgres) and Amazon ElastiCache Serverless (Redis)
serve as the system of record (SOR) and cache, respectively, that the circuit breakers wrap and allow fallback between.
The Amazon DynamoDB table acts as the durable dirty flag store, informing which of the other two datastores a given
read should trust.

## Network & Access

- EKS cluster on Fargate with private subnets and NAT for egress
- AWS Load Balancer Controller provisions an ALB via Kubernetes Ingress
- IRSA grants the Load Balancer Controller and application pod scoped IAM permissions (no static credentials)

## Observability

- Micrometer/Prometheus metrics exposed through Spring Boot Actuator. Includes circuit breaker state metrics
- Structured (ECS format) JSON logging, shipped to CloudWatch for aggregation
- Actuator endpoints isolated on separate management port and never exposed publicly

# Notable Bugs and Resolution

## CoreDNS Fargate Patching Order

Amazon EKS Fargate does not have any EC2 nodes. However, the default deployment for CoreDNS comes annotated with
`eks.amazonaws.com/compute-type: ec2` which causes the Fargate scheduler to ignore them. Resolving this requires patching
the annotation to `fargate` after the cluster exists but before anything else depends on cluster DNS. 

The app's IAM roles and the CoreDNS patch both needed to happen before other infrastructure rolled out. To accomplish this
the Terraform apply was split into two passes: the cluster, Fargate profiles, and the Fargate pod execution role's IAM
policy attachment first, then a CoreDNS patch step then everything else. The most time-consuming part of this effort was
discovering the IAM policy attachment specifically had to be in the first pass. Without it the Fargate pod execution role
had no permissions and thus the CoreDNS pods attempted to pull their image with anonymous, credential-less requests.
Resulting in a misleading `no basic auth credentials` error.

## Incorrect Default Permissions For Cluster Creator

Initial troubleshooting for the CoreDNS patching issue was accomplished through adding debug steps to the `deploy` workflow
in GitHub actions. Eventually, this proved too time-consuming, so access to the EKS cluster through the AWS console was
provisioned. However, upon redeploying with the AWS console access, the GitHub Actions user had lost its access to the
EKS cluster.

This turned out to be similar to a bug previously reported against the `aws_eks_cluster` Terraform resource. Bug reports
(https://github.com/hashicorp/terraform-provider-aws/issues/36259) note this issue should be resolved with version 
5.58.0 of the AWS Terraform provider. However, I encountered a similar symptom with version 6.36.0. Documented behavior 
is that `bootstrap_cluster_creator_admin_permissions` is defaulted to `true`. However, this is not currently the case 
and it has to be explicitly enabled.

    access_config {
      ...
      bootstrap_cluster_creator_admin_permissions = true
    }

# Running Locally

The application is configured to run locally through docker compose.

Run with:

    docker compose up --build

Teardown with:

    docker compose down -v

# Deploying to AWS

The application and required infrastructure can be deployed to AWS using GitHub Actions.

- CI: This workflow triggers whenever code is pushed up to the repo and runs through a Gradle build.
- Deploy: This workflow uses Terraform to deploy all the infrastructure needed for the application to run in AWS
  and then deploys the application into the EKS cluster.
- Destroy: This workflow will undeploy the application from EKS and then teardown all the infrastructure previously
  deployed through Terraform.
- Terraform Plan: This workflow runs a Terraform plan for a pull request affecting the infra directory into the main branch.

# Scope Boundaries

Being that this project is intended as a demo application with limited deployment, several features needed for an actual
production use case were deemed out of scope.

- Authentication / Authorization - The API is left unsecured to allow for a reviewer to test the application without the 
friction of obtaining user credentials. The ephemeral, destroy-between-sessions lifecycle of the application helps to
limit the actual risk.
- HTTPS / TLS - The ALB serves plain HTTP. Terminating TLS would require registering a domain and certificate. Neither
are part of this project's scope.
- High Availability - Both the data tier and the application itself run single instances. This is a reasonable tradeoff 
for infrastructure destroyed and recreated between sessions.
- Backups / Retention - Aurora is configured with `skip_final_snapshot = true` and no backup retention, consistent
with the project's ephemeral nature.

# Testing

This application includes multiple layers of automated testing.

- Unit tests covering logic within individual classes
- A targeted resilience test suite using actual CircuitBreakers and the CircuitBreakerExecutor. This covers all the fallback
scenarios and whether they could trigger the respective CircuitBreakers tripping.
- Integration tests using actual Postgres and DynamoDB Local instances through TestContainers. These tests allow verifying
database interaction and transaction behavior, that would not be covered by unit tests.
- Controller level tests validating the HTTP layer, request validation, response statuses and response bodies.
- Mutation testing using PITest. Mutation testing verifies the quality of the tests, not just how many 
lines they hit.