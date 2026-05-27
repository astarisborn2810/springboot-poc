# SQS Trigger Lambda

Plain Java AWS Lambda that consumes S3 event notifications from `payroll-outbound-trigger-queue-${Environment}` and starts the PEARL downstream Step Functions workflow.

## Handler

Lambda handler:

```text
com.pearl.downstream.trigger.handler.SqsTriggerHandler::handleRequest
```

The handler implements:

```java
RequestHandler<SQSEvent, SQSBatchResponse>
```

## Flow

1. S3 writes an `s3:ObjectCreated:Put` event into SQS when a new object lands under `outbound/`.
2. `SqsTriggerHandler` receives one or more SQS records.
3. `SqsTriggerHandler` returns an SQS partial batch response so one failed message does not replay successfully processed messages.
4. `TriggerMessageProcessor` parses the S3 event body.
5. `s3:TestEvent` messages are ignored safely.
6. Object-created records are validated and converted into `StepFunctionInput`, including `fileName`, `s3PathOrArn`, and a stable S3 idempotency key.
7. `DynamoDbIdempotencyStore`, when `IDEMPOTENCY_TABLE_NAME` is configured, conditionally claims the event before the workflow starts.
8. `StepFunctionStarterService` starts the configured Step Functions state machine with a deterministic execution name derived from the idempotency key.
9. Completed duplicate events are skipped; in-progress duplicates are returned as failed SQS batch items so they retry later.

## Environment Variables

Required:

- `STATE_MACHINE_ARN`: Step Functions state machine ARN to start.

Optional:

- `AWS_REGION`: Region used by AWS SDK v2. If absent, the AWS SDK default region provider chain is used.
- `LOG_LEVEL`: Logback root level. Defaults to `INFO`.
- `IDEMPOTENCY_TABLE_NAME`: DynamoDB table used to prevent duplicate S3 event processing. Strongly recommended for every non-local environment.
- `IDEMPOTENCY_TTL_DAYS`: DynamoDB TTL retention for idempotency records. Defaults to `91`, matching the Step Functions execution-name uniqueness window.
- `IDEMPOTENCY_IN_PROGRESS_TTL_SECONDS`: Lease for a `STARTING` idempotency claim. Defaults to `900`.

Credentials are not configured in code. The Lambda uses the AWS SDK default credentials provider chain, which should resolve from the Lambda execution role in AWS.

## Idempotency Table

Production deployments should set `IDEMPOTENCY_TABLE_NAME` to a DynamoDB table with this shape:

```text
Partition key: idempotencyKey (String)
TTL attribute: expiresAt
Billing mode: PAY_PER_REQUEST
```

The Lambda writes one record per S3 object-created event. The key is derived from bucket, object key, event type, and the S3 `sequencer` when present. This allows duplicate S3 or SQS deliveries to be ignored while allowing a later upload of the same key with a new S3 sequence to start a new workflow.

Record statuses:

- `STARTING`: the Lambda claimed the event and is starting Step Functions.
- `STARTED`: Step Functions was started, or the deterministic execution name already existed.
- `FAILED`: startup failed before completion; a retry can reclaim the event.

## S3 Key Metadata Extraction

The processor attempts to infer metadata from keys shaped like:

```text
outbound/{vendorId}/{dataType}/{batchId}/{batchId}_{vendorName}_{plan}.json
```

Example:

```text
outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json
```

If message attributes are present, they override inferred metadata:

- `correlationId`
- `batchId`
- `vendorId`
- `dataType`

If no `correlationId` is supplied, a UUID is generated.

The Step Functions input carries both the original S3 metadata and the service API file pointer:

- `payloadType`: normalized value used by the state machine choice state, for example `financial`
- `fileName`: base object name without extension, for example `batch-20260522_prismhr_PEARL-401K-PLAN-001`
- `s3PathOrArn`: S3 URI for the object, for example `s3://payroll-outbound-dev/outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json`

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
- `idempotencyKey` inside Step Functions input metadata

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

## How to build Lambda for Java 21

The SQS trigger Lambda must be deployed to the AWS Lambda Java 21 runtime. Its Maven module explicitly compiles with Java 21 source, target, and release settings, and the Maven Enforcer Plugin checks that Lambda bytecode does not exceed Java 21 compatibility.

Build the production Lambda artifact from the repository root:

```powershell
mvn -pl services/sqs-trigger-lambda clean package
```

Deploy this shaded JAR to AWS Lambda:

```text
services/sqs-trigger-lambda/target/sqs-trigger-lambda-aws.jar
```

The Lambda handler remains:

```text
com.pearl.downstream.trigger.handler.SqsTriggerHandler::handleRequest
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
          "key": "outbound/prismhr/financial/batch-20260522/batch-20260522_prismhr_PEARL-401K-PLAN-001.json",
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

This Lambda uses `RequestHandler<SQSEvent, SQSBatchResponse>` and requires the Lambda event source mapping to include `ReportBatchItemFailures`.

Negative-scenario handling:

- Empty events return success with no failed items.
- S3 test events are ignored safely.
- Malformed JSON, missing S3 fields, unsupported S3 event types, DynamoDB claim failures, and Step Functions start failures return only that SQS message id in `batchItemFailures`.
- Successfully processed records in the same Lambda invocation are not retried when another SQS message fails.
- Duplicate completed S3 events are logged and skipped.
- Active in-progress duplicates retry later instead of being deleted prematurely.
- Step Functions `ExecutionAlreadyExists` is treated as duplicate success because execution names are deterministic.
- Repeated permanent failures still move to the SQS DLQ through the queue redrive policy.

For a 10,000-file S3 spike, SQS buffers the fan-in, Lambda polls in batches, partial batch response isolates failures, and DynamoDB/Step Functions idempotency prevents duplicate workflow starts from at-least-once S3/SQS delivery.
