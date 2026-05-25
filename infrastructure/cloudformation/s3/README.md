# PEARL Payroll S3 CloudFormation

This folder contains the S3 CloudFormation template for PEARL payroll platform payload storage.

## Template

`payroll-s3-buckets.yaml` creates two environment-scoped S3 buckets:

- `payroll-inbound-${Environment}` stores raw inbound vendor payloads.
- `payroll-outbound-${Environment}` stores BizNuvo translated outbound payloads.

Each bucket is configured with:

- Block all public access
- Bucket-owner-enforced object ownership
- Versioning enabled
- Server-side encryption with Amazon S3 managed AES256 encryption
- Lifecycle transition to GLACIER after 30 days
- Lifecycle expiration after 365 days
- Tags for `Project`, `Environment`, and `ManagedBy`

The template does not create SQS notifications, hardcode AWS account IDs, or include credentials.

## Validate

From the repository root:

```powershell
aws cloudformation validate-template `
  --template-body file://infrastructure/cloudformation/s3/payroll-s3-buckets.yaml `
  --region ap-south-1
```

## Deploy

Sample deployment for `dev` in `ap-south-1`:

```powershell
aws cloudformation deploy `
  --template-file infrastructure/cloudformation/s3/payroll-s3-buckets.yaml `
  --stack-name pearl-payroll-s3-dev `
  --parameter-overrides Environment=dev `
  --region ap-south-1 `
  --no-fail-on-empty-changeset
```

Use the same template for `qa`, `uat`, and `prod` by changing `Environment` and `--stack-name`.

## Delete Safely

S3 buckets must be empty before CloudFormation can delete them. Before deleting a stack:

1. Confirm the environment and stack name.
2. Export or retain any payloads needed for audit or replay.
3. Empty current and noncurrent object versions from both buckets.
4. Delete the CloudFormation stack.

Example delete command for `dev`:

```powershell
aws cloudformation delete-stack `
  --stack-name pearl-payroll-s3-dev `
  --region ap-south-1
```

Then wait for deletion to complete:

```powershell
aws cloudformation wait stack-delete-complete `
  --stack-name pearl-payroll-s3-dev `
  --region ap-south-1
```

## Notes

S3 bucket names are globally unique. If `payroll-inbound-dev` or `payroll-outbound-dev` already exists in another AWS account, deployment will fail and the platform naming standard must be adjusted before promotion.
