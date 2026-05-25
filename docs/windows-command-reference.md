# Windows Command Reference

Create the monorepo folders:

```powershell
New-Item -ItemType Directory -Force -Path @(
  "services/financial-processing-service",
  "services/indicative-processing-service",
  "services/result-tracking-service",
  "services/batch-completion-service",
  "services/config-service",
  "services/datatype-router-lambda",
  "services/sqs-trigger-lambda",
  "services/common-library",
  "infrastructure/cloudformation",
  "infrastructure/ecs",
  "infrastructure/lambda",
  "infrastructure/s3",
  "infrastructure/sqs",
  "infrastructure/dynamodb",
  "infrastructure/iam",
  "local/docker-compose",
  "local/postgres",
  "local/localstack",
  "payloads/financial",
  "payloads/indicative",
  "step-functions",
  "scripts",
  "docs",
  ".github/workflows",
  "Jenkins"
)
```

Git setup:

```powershell
git init
git branch -M main
git remote add origin https://github.com/<github-organization>/pearl-payroll-platform.git
git add .
git commit -m "Initial PEARL payroll platform monorepo"
git push -u origin main
```

Maven:

```powershell
java -version
mvn -version
mvn clean verify
mvn -pl services/common-library test
mvn -pl services/financial-processing-service -am spring-boot:run
```

Docker:

```powershell
docker version
docker compose -f local/docker-compose/docker-compose.yml up -d
docker compose -f local/docker-compose/docker-compose.yml ps
docker compose -f local/docker-compose/docker-compose.yml down
.\scripts\docker-build-services.ps1 -ImageTag local
```

GitHub CLI:

```powershell
gh auth login
gh repo create pearl-payroll-platform --private --source . --remote origin --push
```
