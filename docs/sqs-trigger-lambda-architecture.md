# SQS Trigger Lambda Architecture

## Architecture Class Diagram

```mermaid
classDiagram
    direction LR

    class SqsTriggerHandler {
        +handleRequest(SQSEvent, Context) SQSBatchResponse
        -batchFailure(SQSMessage) BatchItemFailure
    }

    class TriggerMessageProcessor {
        +process(SQSMessage) List~StepFunctionInput~
        -toS3EventMessage(JsonNode, SQSMessage) S3EventMessage
        -startStepFunction(StepFunctionInput, S3EventMessage, IdempotencyClaim) StepFunctionStartResult
    }

    class MessageValidator {
        +validate(S3EventMessage) void
    }

    class CorrelationService {
        +correlationFor(SQSMessage, String) CorrelationMetadata
    }

    class StepFunctionStarterService {
        +startExecution(StepFunctionInput) StepFunctionStartResult
        -executionName(StepFunctionInput) String
    }

    class IdempotencyStore {
        <<interface>>
        +claim(S3EventMessage, StepFunctionInput) IdempotencyClaim
        +markStarted(IdempotencyClaim, StepFunctionStartResult) void
        +release(IdempotencyClaim, Throwable) void
    }

    class DynamoDbIdempotencyStore {
        +claim(S3EventMessage, StepFunctionInput) IdempotencyClaim
        +markStarted(IdempotencyClaim, StepFunctionStartResult) void
        +release(IdempotencyClaim, Throwable) void
    }

    class NoOpIdempotencyStore {
        +claim(S3EventMessage, StepFunctionInput) IdempotencyClaim
        +markStarted(IdempotencyClaim, StepFunctionStartResult) void
        +release(IdempotencyClaim, Throwable) void
    }

    class IdempotencyKeyUtil {
        +forEvent(S3EventMessage) String
        +forInput(StepFunctionInput) String
        +shortHash(String, int) String
    }

    class JsonLogger {
        +info(String, S3EventMessage) void
        +warn(String, S3EventMessage) void
        +error(String, S3EventMessage, Throwable) void
        +error(String, String, Throwable) void
    }

    class S3EventMessage {
        <<record>>
        bucketName String
        objectKey String
        eventType String
        eTag String
        sequencer String
        sourceMessageId String
    }

    class StepFunctionInput {
        <<record>>
        correlationId String
        batchId String
        vendorId String
        dataType String
        payloadType String
        fileName String
        s3PathOrArn String
        metadata Map
    }

    class IdempotencyClaim {
        <<record>>
        idempotencyKey String
        decision Decision
        existingStatus String
    }

    class StepFunctionStartResult {
        <<record>>
        executionName String
        executionArn String
        alreadyExists boolean
    }

    class DynamoDbClient {
        <<AWS SDK>>
    }

    class SfnClient {
        <<AWS SDK>>
    }

    SqsTriggerHandler --> TriggerMessageProcessor : delegates each SQS record
    SqsTriggerHandler --> JsonLogger : logs failures
    TriggerMessageProcessor --> MessageValidator : validates S3 event
    TriggerMessageProcessor --> CorrelationService : extracts metadata
    TriggerMessageProcessor --> IdempotencyStore : claims or skips event
    TriggerMessageProcessor --> StepFunctionStarterService : starts workflow
    TriggerMessageProcessor --> JsonLogger : structured logs
    TriggerMessageProcessor --> S3EventMessage : creates
    TriggerMessageProcessor --> StepFunctionInput : creates
    TriggerMessageProcessor --> IdempotencyClaim : evaluates
    TriggerMessageProcessor --> StepFunctionStartResult : evaluates
    StepFunctionInput --> IdempotencyKeyUtil : creates stable key
    StepFunctionStarterService --> IdempotencyKeyUtil : deterministic execution name
    StepFunctionStarterService --> SfnClient : StartExecution
    DynamoDbIdempotencyStore ..|> IdempotencyStore
    NoOpIdempotencyStore ..|> IdempotencyStore
    DynamoDbIdempotencyStore --> DynamoDbClient : conditional writes
```

## Solution Diagram

```mermaid
flowchart TD
    A["10,000 files uploaded under S3 outbound/ prefix"] --> B["S3 ObjectCreated events"]
    B --> C["SQS Standard Queue<br/>payroll-outbound-trigger-queue"]
    C --> D["Lambda Event Source Mapping<br/>BatchSize 10<br/>ReportBatchItemFailures enabled"]
    D --> E["SqsTriggerHandler<br/>returns SQSBatchResponse"]

    E --> F{"SQS event empty?"}
    F -- "Yes" --> G["Return success<br/>no failed items"]
    F -- "No" --> H["Process each SQS record independently"]

    H --> I{"S3 TestEvent?"}
    I -- "Yes" --> J["Ignore safely<br/>delete SQS message"]
    I -- "No" --> K["Parse S3 record<br/>decode bucket/key/event/eTag/sequencer"]

    K --> L{"Valid ObjectCreated event?"}
    L -- "No" --> M["Return this messageId in batchItemFailures<br/>SQS retries then DLQ after redrive limit"]
    L -- "Yes" --> N["Build StepFunctionInput<br/>fileName + s3PathOrArn + metadata"]

    N --> O["Create stable idempotency key<br/>bucket + key + event + sequencer/eTag/size/time"]
    O --> P{"IDEMPOTENCY_TABLE_NAME configured?"}

    P -- "No" --> Q["NoOp idempotency<br/>local/dev mode"]
    P -- "Yes" --> R["DynamoDB conditional PutItem<br/>status STARTING + leaseExpiresAt + expiresAt"]

    R --> S{"Claim result"}
    S -- "STARTED duplicate" --> T["Skip duplicate<br/>delete SQS message"]
    S -- "STARTING in progress" --> U["Return this messageId in batchItemFailures<br/>retry after visibility timeout"]
    S -- "Claim acquired" --> V["Start Step Functions"]
    Q --> V

    V --> W["Deterministic execution name<br/>vendor-dataType-batch-hash"]
    W --> X{"StartExecution result"}
    X -- "Started" --> Y["Mark DynamoDB STARTED<br/>store executionName/executionArn"]
    X -- "ExecutionAlreadyExists" --> Z["Treat as duplicate success<br/>mark STARTED"]
    X -- "Retryable/error" --> AA["Mark DynamoDB FAILED<br/>return messageId in batchItemFailures"]

    Y --> AB["SQS deletes successful message"]
    Z --> AB
    T --> AB
    AA --> AC["SQS retries only failed message"]
    M --> AC
    U --> AC
    AC --> AD["DLQ if maxReceiveCount exceeded"]

    V --> AE["Step Functions state machine"]
    AE --> AF{"payloadType"}
    AF -- "financial" --> AG["Financial ECS task/API<br/>receives fileName + s3PathOrArn"]
    AF -- "indicative" --> AH["Indicative ECS task/API<br/>receives fileName + s3PathOrArn"]
    AF -- "unknown" --> AI["Manual review/fail state"]
```

## Negative Scenario Coverage

| Scenario | Handling |
| --- | --- |
| 10,000 files arrive together | SQS buffers events; Lambda drains in batches. |
| One SQS record fails in a batch | Only that `messageId` is returned in `batchItemFailures`. |
| Duplicate S3/SQS event | DynamoDB `STARTED` record skips duplicate. |
| Same event already being started | DynamoDB `STARTING` record causes retry later. |
| Lambda crashes after claim before Step Functions | `leaseExpiresAt` allows reclaim after the in-progress lease expires. |
| Step Functions duplicate execution name | `ExecutionAlreadyExists` is treated as duplicate success. |
| Malformed JSON or missing S3 fields | Failed message retries and eventually moves to DLQ. |
| DynamoDB temporarily unavailable | Message retries through SQS. |
| Step Functions temporarily unavailable | Claim is marked `FAILED`; message retries. |
| Permanent poison message | SQS redrive policy moves it to DLQ. |
