# PEARL Payroll Platform

Enterprise monorepo for the PEARL payroll downstream platform. The repo contains Spring Boot services, Java Lambda functions, shared platform libraries, local development infrastructure, AWS deployment placeholders, and CI/CD scaffolding.

## Architecture Overview

```text
Inbound payloads
  -> datatype-router-lambda
  -> Step Functions payroll-downstream-state-machine
  -> ECS EC2 Spring Boot services
  -> PostgreSQL, DynamoDB, S3, SQS
  -> result-tracking-service
  -> batch-completion-service
```

The platform separates financial payroll processing from indicative employee and benefits processing. Shared contracts, correlation IDs, logging, and telemetry helpers live in `services/common-library`.

## Services

| Module | Type | Responsibility |
| --- | --- | --- |
| `common-library` | Java library | Shared DTOs, constants, exceptions, MDC, correlation, telemetry helpers |
| `financial-processing-service` | Spring Boot ECS service | Payroll records, deductions, taxes, contributions |
| `indicative-processing-service` | Spring Boot ECS service | Employee demographics, participant details, benefits |
| `result-tracking-service` | Spring Boot ECS service | Batch status, audit state, downstream milestone tracking |
| `batch-completion-service` | Spring Boot ECS service | Completion events, finalization, downstream notifications |
| `config-service` | Spring Boot ECS service | Runtime configuration and feature controls |
| `datatype-router-lambda` | Java Lambda | Routes financial and indicative payloads |
| `sqs-trigger-lambda` | Java Lambda | Handles SQS-driven orchestration triggers |

## Technology Baseline

- Java 24 compile target
- Spring Boot 3.5.x
- Maven multi-module reactor
- Docker and Docker Compose
- PostgreSQL and DynamoDB
- AWS ECS on EC2
- AWS Lambda Java container images
- S3, SQS, Step Functions, IAM
- OpenTelemetry, Spring Actuator, JSON logs
- Windows and IntelliJ compatible

Spring Boot services are pinned to the 3.5 line for Java 24 compatibility. The `sqs-trigger-lambda` module is compiled separately for AWS Lambda Java 21 runtime compatibility.

## Quick Start On Windows

```powershell
Copy-Item .env.example .env
.\scripts\start-local.ps1
mvn clean verify
mvn -pl services/financial-processing-service -am spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/v1/financial/status
```

OpenAPI contracts:

```powershell
Invoke-RestMethod http://localhost:8081/v3/api-docs
Invoke-RestMethod http://localhost:8082/v3/api-docs
```

Swagger UI is available at `http://localhost:8081/swagger-ui.html` and `http://localhost:8082/swagger-ui.html` when the financial and indicative services are running.

Financial and indicative Step Functions calls pass only an S3 file pointer:

```json
{
  "fileName": "batch-20260522_prismhr_PEARL-401K-PLAN-001",
  "s3PathOrArn": "s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json"
}
```

The `fileName` must use `batchId_vendorName_plan` format. Financial external API request count is calculated after reading the S3 file, from the combined record count:

```text
expectedExternalApiRequests = ceil((payrollRecords.size + controlTotalRecords.size) / 500)
```

Stop local dependencies:

```powershell
.\scripts\stop-local.ps1
```

## Maven Setup

The root `pom.xml` is the Maven aggregator and parent. It centralizes Java 24, Spring Boot dependency management, AWS SDK dependency management, OpenTelemetry dependency management, compiler settings, test plugins, and packaging plugins.

```powershell
mvn clean verify
mvn -pl services/common-library test
mvn -pl services/datatype-router-lambda -am package
```

## Docker Setup

Local dependencies:

```powershell
docker compose -f local/docker-compose/docker-compose.yml up -d
docker compose -f local/docker-compose/docker-compose.yml ps
```

Build service images after Maven packaging:

```powershell
mvn -DskipTests package
.\scripts\docker-build-services.ps1 -ImageTag local
```

Local endpoints:

| Component | URL |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| pgAdmin | `http://localhost:5050` |
| LocalStack | `http://localhost:4566` |
| OTEL HTTP | `http://localhost:4318` |
| OTEL Prometheus metrics | `http://localhost:9464/metrics` |

## AWS Setup

```powershell
aws configure sso
aws sts get-caller-identity --profile pearl-dev
aws s3 sync infrastructure s3://<artifact-bucket-name>/infrastructure --profile pearl-dev
```

Deploy the root placeholder stack:

```powershell
aws cloudformation deploy `
  --stack-name pearl-payroll-dev-root `
  --template-file infrastructure/cloudformation/root-stack.yml `
  --parameter-overrides file://infrastructure/cloudformation/parameters-dev.json `
  --capabilities CAPABILITY_NAMED_IAM `
  --profile pearl-dev
```

## ECS Deployment Flow

1. Build with Maven.
2. Build Docker images per service.
3. Scan images and generate SBOMs.
4. Push immutable image tags to ECR.
5. Render task definitions from `infrastructure/ecs/task-definition-template.json`.
6. Deploy CloudFormation service stacks.
7. Update ECS services.
8. Verify `/actuator/health` and rollback alarms.

## Step Functions Overview

`step-functions/payroll-downstream-state-machine.asl.json` routes payloads by `payloadType`:

- `financial` routes to `financial-processing-service`
- `indicative` routes to `indicative-processing-service`
- successful processing flows through result tracking and batch completion
- unsupported payloads fail into manual review handling

## Git Repository Setup

```powershell
git init
git branch -M main
git remote add origin https://github.com/<github-organization>/pearl-payroll-platform.git
git add .
git commit -m "Initial PEARL payroll platform monorepo"
git push -u origin main
```

Branch model:

- `main`
- `develop`
- `feature/*`
- `release/*`
- `hotfix/*`

See `docs/branching-strategy.md` for the enterprise workflow.

## IntelliJ Setup

1. Open the root `pom.xml` as a Maven project.
2. Set Project SDK to Java 24.
3. Enable annotation processors.
4. Enable Docker, AWS Toolkit, GitHub, and Maven plugins.
5. Use the Maven tool window to run root lifecycle goals.
6. Use `.env.example` values for local service run configurations.

Full guide: `docs/intellij-setup.md`.

## Documentation

- `docs/folder-structure.md`
- `docs/git-commands.md`
- `docs/branching-strategy.md`
- `docs/intellij-setup.md`
- `docs/local-development.md`
- `docs/directory-standards.md`
- `docs/aws-setup.md`
- `docs/observability.md`
- `docs/cicd.md`
- `docs/windows-command-reference.md`
