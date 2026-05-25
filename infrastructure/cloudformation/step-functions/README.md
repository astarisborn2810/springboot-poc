# PEARL Payroll Step Functions CloudFormation

This folder contains the placeholder Step Functions stack for PEARL downstream orchestration.

## Template

`downstream-placeholder-stepfunction.yaml` creates:

- Standard Step Functions state machine named `payroll-downstream-orchestrator-${Environment}`
- IAM execution role for Step Functions
- CloudWatch Logs log group for execution logging
- Simple placeholder ASL workflow:
  - `DetermineDataTypePass`
  - `Success`

The template is intended for deployment in `ap-south-1`.

## Validate

```powershell
aws cloudformation validate-template `
  --template-body file://infrastructure/cloudformation/step-functions/downstream-placeholder-stepfunction.yaml `
  --region ap-south-1
```

## Deploy

Sample deployment for `dev`:

```powershell
aws cloudformation deploy `
  --template-file infrastructure/cloudformation/step-functions/downstream-placeholder-stepfunction.yaml `
  --stack-name pearl-payroll-stepfunctions-dev `
  --parameter-overrides Environment=dev `
  --capabilities CAPABILITY_NAMED_IAM `
  --region ap-south-1 `
  --no-fail-on-empty-changeset
```

`CAPABILITY_NAMED_IAM` is required because the template creates a named IAM role for Step Functions.

## Outputs

After deployment, retrieve the state machine ARN:

```powershell
aws cloudformation describe-stacks `
  --stack-name pearl-payroll-stepfunctions-dev `
  --region ap-south-1 `
  --query "Stacks[0].Outputs[?OutputKey=='StateMachineArn'].OutputValue" `
  --output text
```

Use that ARN as `STATE_MACHINE_ARN` for `sqs-trigger-lambda`.
