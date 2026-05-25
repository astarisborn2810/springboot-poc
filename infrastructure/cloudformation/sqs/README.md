# PEARL Payroll SQS CloudFormation

This folder contains the SQS CloudFormation template for PEARL payroll outbound trigger processing.

## Template

`payroll-sqs.yaml` creates two standard SQS queues:

- `payroll-outbound-trigger-queue-${Environment}` receives outbound payload processing trigger messages.
- `payroll-outbound-trigger-dlq-${Environment}` stores messages that fail processing repeatedly.

The main queue is configured with:

- Standard queue behavior, not FIFO
- `VisibilityTimeout` of 300 seconds
- `MessageRetentionPeriod` of 14 days
- `ReceiveMessageWaitTimeSeconds` of 20 seconds for long polling
- Managed server-side encryption for SQS
- Redrive to the DLQ after 5 failed receive attempts
- Tags for `Project`, `Environment`, and `ManagedBy`

The template does not include AWS credentials and does not hardcode an AWS account ID.

## How The DLQ Works

Consumers receive messages from the main queue. If a consumer fails to delete a message before the visibility timeout expires, SQS makes the message visible again for another attempt.

After `maxReceiveCount` reaches `5`, SQS moves the message to `payroll-outbound-trigger-dlq-${Environment}`. Operators can inspect DLQ messages to understand poison payloads, failed BizNuvo outbound translations, schema problems, transient downstream outages, or idempotency failures.

The DLQ uses a redrive allow policy so only the matching outbound trigger queue for the same environment can redrive messages into it.

## Validate

From the repository root:

```powershell
aws cloudformation validate-template `
  --template-body file://infrastructure/cloudformation/sqs/payroll-sqs.yaml `
  --region ap-south-1
```

## Deploy

Sample deployment for `dev` in `ap-south-1`:

```powershell
aws cloudformation deploy `
  --template-file infrastructure/cloudformation/sqs/payroll-sqs.yaml `
  --stack-name pearl-payroll-sqs-dev `
  --parameter-overrides Environment=dev `
  --region ap-south-1 `
  --no-fail-on-empty-changeset
```

Use the same template for `qa`, `uat`, and `prod` by changing `Environment` and `--stack-name`.

## Inspect Messages

Get the main queue URL from stack outputs:

```powershell
aws cloudformation describe-stacks `
  --stack-name pearl-payroll-sqs-dev `
  --region ap-south-1 `
  --query "Stacks[0].Outputs[?OutputKey=='MainQueueUrl'].OutputValue" `
  --output text
```

Peek at messages in the main queue:

```powershell
aws sqs receive-message `
  --queue-url "<MAIN_QUEUE_URL>" `
  --max-number-of-messages 10 `
  --wait-time-seconds 5 `
  --attribute-names All `
  --message-attribute-names All `
  --region ap-south-1
```

Get the DLQ URL:

```powershell
aws cloudformation describe-stacks `
  --stack-name pearl-payroll-sqs-dev `
  --region ap-south-1 `
  --query "Stacks[0].Outputs[?OutputKey=='DeadLetterQueueUrl'].OutputValue" `
  --output text
```

Inspect DLQ messages:

```powershell
aws sqs receive-message `
  --queue-url "<DEAD_LETTER_QUEUE_URL>" `
  --max-number-of-messages 10 `
  --wait-time-seconds 5 `
  --attribute-names All `
  --message-attribute-names All `
  --region ap-south-1
```

Receiving a message does not delete it unless the consumer calls `delete-message` with the receipt handle. Be careful when inspecting production queues.

## Delete Safely

Before deleting the stack:

1. Confirm the environment and stack name.
2. Stop active producers and consumers.
3. Drain or archive messages from the main queue and DLQ if they are needed for audit or replay.
4. Delete the CloudFormation stack.

Example delete command for `dev`:

```powershell
aws cloudformation delete-stack `
  --stack-name pearl-payroll-sqs-dev `
  --region ap-south-1
```

Then wait for deletion to complete:

```powershell
aws cloudformation wait stack-delete-complete `
  --stack-name pearl-payroll-sqs-dev `
  --region ap-south-1
```
