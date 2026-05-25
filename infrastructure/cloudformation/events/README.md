# PEARL Payroll S3 To SQS Notification

This folder contains the CloudFormation template that connects the PEARL outbound payload bucket to the outbound trigger SQS queue.

## Template

`payroll-s3-to-sqs-notification.yaml` configures:

- An SQS queue policy allowing `s3.amazonaws.com` to send messages from the outbound bucket.
- An S3 notification on the existing outbound bucket for `s3:ObjectCreated:Put`.
- A prefix filter of `outbound/`.
- The existing outbound trigger queue as the notification destination.

The template does not recreate the S3 bucket or SQS queue. It accepts the bucket and queue names, ARNs, and URL as parameters.

CloudFormation does not provide a standalone native resource for adding notifications to an existing S3 bucket. This template uses a Lambda-backed custom resource to apply and remove only the PEARL-managed notification entry while preserving unrelated bucket notifications.

## Deployment Order

Deploy stacks in this order:

1. Deploy S3: `infrastructure/cloudformation/s3/payroll-s3-buckets.yaml`
2. Deploy SQS: `infrastructure/cloudformation/sqs/payroll-sqs.yaml`
3. Deploy notification: `infrastructure/cloudformation/events/payroll-s3-to-sqs-notification.yaml`

## Get Bucket Name And ARN

Example for `dev` in `ap-south-1`:

```powershell
$Region = "ap-south-1"
$Environment = "dev"
$S3StackName = "pearl-payroll-s3-dev"

$OutboundBucketName = aws cloudformation describe-stacks `
  --stack-name $S3StackName `
  --region $Region `
  --query "Stacks[0].Outputs[?OutputKey=='OutboundBucketName'].OutputValue" `
  --output text

$OutboundBucketArn = aws cloudformation describe-stacks `
  --stack-name $S3StackName `
  --region $Region `
  --query "Stacks[0].Outputs[?OutputKey=='OutboundBucketArn'].OutputValue" `
  --output text
```

## Get Queue ARN And URL

```powershell
$SqsStackName = "pearl-payroll-sqs-dev"

$OutboundQueueArn = aws cloudformation describe-stacks `
  --stack-name $SqsStackName `
  --region $Region `
  --query "Stacks[0].Outputs[?OutputKey=='MainQueueArn'].OutputValue" `
  --output text

$OutboundQueueUrl = aws cloudformation describe-stacks `
  --stack-name $SqsStackName `
  --region $Region `
  --query "Stacks[0].Outputs[?OutputKey=='MainQueueUrl'].OutputValue" `
  --output text
```

## Validate

```powershell
aws cloudformation validate-template `
  --template-body file://infrastructure/cloudformation/events/payroll-s3-to-sqs-notification.yaml `
  --region ap-south-1
```

## Deploy

```powershell
aws cloudformation deploy `
  --template-file infrastructure/cloudformation/events/payroll-s3-to-sqs-notification.yaml `
  --stack-name pearl-payroll-events-dev `
  --parameter-overrides `
      Environment=$Environment `
      OutboundBucketName=$OutboundBucketName `
      OutboundBucketArn=$OutboundBucketArn `
      OutboundQueueArn=$OutboundQueueArn `
      OutboundQueueUrl=$OutboundQueueUrl `
  --capabilities CAPABILITY_NAMED_IAM `
  --region $Region `
  --no-fail-on-empty-changeset
```

`CAPABILITY_NAMED_IAM` is required because the template creates an IAM role for the custom resource Lambda.

## Test Upload

Upload a test file under the `outbound/` prefix:

```powershell
Set-Content -Path .\outbound-notification-test.json -Value '{"event":"pearl-outbound-test"}'

aws s3 cp .\outbound-notification-test.json `
  s3://$OutboundBucketName/outbound/outbound-notification-test.json `
  --region $Region
```

Only `PutObject` events under the `outbound/` prefix should send messages to SQS.

## Check The SQS Message

```powershell
aws sqs receive-message `
  --queue-url $OutboundQueueUrl `
  --max-number-of-messages 10 `
  --wait-time-seconds 20 `
  --attribute-names All `
  --message-attribute-names All `
  --region $Region
```

The SQS message body should contain an S3 event notification with:

- `eventName` similar to `ObjectCreated:Put`
- `s3.bucket.name` equal to `payroll-outbound-${Environment}`
- `s3.object.key` beginning with `outbound/`

Receiving a message does not delete it. Delete the test message only after capturing the receipt handle:

```powershell
aws sqs delete-message `
  --queue-url $OutboundQueueUrl `
  --receipt-handle "<RECEIPT_HANDLE>" `
  --region $Region
```

## Delete Safely

Delete the notification stack before deleting the SQS or S3 stacks:

```powershell
aws cloudformation delete-stack `
  --stack-name pearl-payroll-events-dev `
  --region $Region

aws cloudformation wait stack-delete-complete `
  --stack-name pearl-payroll-events-dev `
  --region $Region
```

On stack delete, the custom resource removes only the notification id managed by this stack and leaves other bucket notifications intact.
