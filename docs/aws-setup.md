# AWS Setup

Configure AWS CLI:

```powershell
aws configure sso
aws sts get-caller-identity --profile pearl-dev
```

Bootstrap artifact bucket:

```powershell
aws s3 mb s3://<artifact-bucket-name> --region us-east-1 --profile pearl-dev
aws s3 sync infrastructure s3://<artifact-bucket-name>/infrastructure --profile pearl-dev
```

Deploy base stack:

```powershell
aws cloudformation deploy `
  --stack-name pearl-payroll-dev-root `
  --template-file infrastructure/cloudformation/root-stack.yml `
  --parameter-overrides file://infrastructure/cloudformation/parameters-dev.json `
  --capabilities CAPABILITY_NAMED_IAM `
  --profile pearl-dev
```

ECS deployment flow:

1. Build Maven artifacts.
2. Build Docker images per service.
3. Scan images.
4. Push immutable tags to ECR.
5. Render ECS task definitions with the image tag.
6. Update ECS services.
7. Verify `/actuator/health`.
8. Promote the same image tag through higher environments.

Lambda deployment flow:

1. Build shaded Lambda jars.
2. Build Lambda container images.
3. Push images to ECR.
4. Update Lambda function image URIs.
5. Publish versions and move aliases by environment.
