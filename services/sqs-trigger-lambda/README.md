# SQS Trigger Lambda

Plain Java AWS Lambda that consumes S3 event notifications from `payroll-outbound-trigger-queue-${Environment}` and starts the PEARL downstream Step Functions workflow.

## Handler

Lambda handler:

```text
com.pearl.downstream.trigger.handler.SqsTriggerHandler::handleRequest
```

The handler implements:

```java
RequestHandler<SQSEvent, Void>
```

## Flow

1. S3 writes an `s3:ObjectCreated:Put` event into SQS when a new object lands under `outbound/`.
2. `SqsTriggerHandler` receives one or more SQS records.
3. `TriggerMessageProcessor` parses the S3 event body.
4. `s3:TestEvent` messages are ignored safely.
5. Object-created records are validated and converted into `StepFunctionInput`.
6. `StepFunctionStarterService` starts the configured Step Functions state machine.

## Environment Variables

Required:

- `STATE_MACHINE_ARN`: Step Functions state machine ARN to start.

Optional:

- `AWS_REGION`: Region used by AWS SDK v2. If absent, the AWS SDK default region provider chain is used.
- `LOG_LEVEL`: Logback root level. Defaults to `INFO`.

Credentials are not configured in code. The Lambda uses the AWS SDK default credentials provider chain, which should resolve from the Lambda execution role in AWS.

## S3 Key Metadata Extraction

The processor attempts to infer metadata from keys shaped like:

```text
outbound/{vendorId}/{dataType}/{batchId}/{fileName}
```

Example:

```text
outbound/prismhr/financial/batch-20260522/payroll-file.json
```

If message attributes are present, they override inferred metadata:

- `correlationId`
- `batchId`
- `vendorId`
- `dataType`

If no `correlationId` is supplied, a UUID is generated.

## Structured Logging

Logs are emitted as single-line JSON and include:

- `correlationId`
- `batchId`
- `vendorId`
- `dataType`
- `bucket`
- `key`
- `eventType`
- `sourceMessageId`

## Build

From the repository root:

```powershell
mvn -pl services/sqs-trigger-lambda test
mvn -pl services/sqs-trigger-lambda package
```

The deployable shaded artifact is:

```text
services/sqs-trigger-lambda/target/sqs-trigger-lambda-aws.jar
```

## Local Test Payloads

S3 test event:

```json
{
  "Service": "Amazon S3",
  "Event": "s3:TestEvent",
  "Time": "2026-05-24T10:00:00.000Z",
  "Bucket": "payroll-outbound-dev"
}
```

Object-created event body inside an SQS record:

```json
{
  "Records": [
    {
      "eventSource": "aws:s3",
      "awsRegion": "ap-south-1",
      "eventTime": "2026-05-24T10:00:00.000Z",
      "eventName": "ObjectCreated:Put",
      "s3": {
        "bucket": {
          "name": "payroll-outbound-dev",
          "arn": "arn:aws:s3:::payroll-outbound-dev"
        },
        "object": {
          "key": "outbound/prismhr/financial/batch-20260522/payroll-file.json",
          "size": 4096,
          "eTag": "etag-test",
          "sequencer": "00664F1D2A5A"
        }
      }
    }
  ]
}
```

## Failure Behavior

This Lambda uses `RequestHandler<SQSEvent, Void>`, so any processing exception is rethrown to let Lambda/SQS retry the batch. Invalid event payloads are rejected with `InvalidTriggerMessageException`. Repeated failures are handled by the SQS redrive policy and moved to the configured DLQ.
