# PEARL Payroll Mock Payloads

This directory contains production-grade synthetic payloads for local development, integration testing, Step Functions orchestration testing, ECS worker testing, S3 upload tests, SQS pointer-message tests, and large payload splitting validation.

The payloads are generated from Java code in `services/common-library/src/main/java/com/pearl/common/test/generator` using `net.datafaker:datafaker` with reproducible random seeds. This gives PEARL Java tests the same style of dynamic mock-data generation that Node.js teams commonly get from Chance/Faker-style libraries.

## Generated Files

| File | Purpose | Records | Size |
| --- | --- | ---: | ---: |
| `payloads/financial/financial-payload-large.json` | PrismHR-style payroll financial contribution extract | 5,000 members across 20 pages | ~10 MB |
| `payloads/indicative/indicative-payload-large.json` | PrismHR-style participant demographics and benefits extract | 5,000 entities | ~17.7 MB |
| `payloads/payload-metrics.json` | File metrics and chunking recommendations | n/a | n/a |

Scenario payloads are stored under:

- `payloads/financial/scenarios`
- `payloads/indicative/scenarios`

Each scenario directory contains:

- `*-valid-small.json`
- `*-malformed-records.json`
- `*-retry-scenarios.json`
- `*-partial-failure.json`

## Financial Payload

The financial payload models 401k and retirement contribution processing from a PrismHR-style payroll extract.

Top-level fields:

- `correlationId`
- `batchId`
- `vendorKey`
- `planId`
- `payrollDate`
- `payPeriodStart`
- `payPeriodEnd`
- `processingTimestamp`
- `totalPages`
- `pages`

Each page contains page metadata, previous and next page references, simulated sub-pages, and 250 member records.

Each member contains:

- `identity`: employee id, participant id, masked SSN, name, employment status, hire date, termination date
- `payrollDetails`: gross, net, overtime, bonus, commission, holiday pay
- `financialContributions`: pre-tax 401k, Roth 401k, after-tax, match, profit sharing, safe harbor, loan repayment
- `taxes`: federal, state, Medicare, Social Security
- `deductions`: medical, dental, vision, garnishment, HSA, FSA
- `employmentData`: department, division, location, payroll group, business unit
- `audit`: source system, source file, record timestamp, schema version
- `scenarioFlags`: malformed, partial, retry, high earner, loan repayment, edge-case tags
- `retrySimulation`: retryable AWS/SQS failure metadata when applicable

Financial edge cases include:

- Negative gross pay for malformed records
- Missing employee ids for partial records
- Terminated employees with current-period payroll
- High earners with elevated contribution and tax profiles
- Active 401k loan repayments
- Retryable SQS visibility timeout simulations

## Indicative Payload

The indicative payload models participant demographic, employment, dependent, beneficiary, benefits, and retirement eligibility data.

Top-level fields:

- `correlationId`
- `batchId`
- `vendorKey`
- `planId`
- `processingTimestamp`
- `entityCount`
- `entities`

Each entity contains:

- `identity`: participant id, employee id, masked SSN, name, suffix
- `personalDetails`: gender, marital status, date of birth, language, citizenship status
- `contactDetails`: personal email, work email, mobile phone, home phone
- `addresses`: home and mailing address with validation status
- `employmentDetails`: hire date, termination date, status, type, union status, compensation type
- `benefits`: medical, dental, vision, disability, life insurance
- `dependents`: spouse, children, beneficiaries
- `retirementDetails`: vesting, eligibility, enrollment, auto-enrollment
- `auditMetadata`: source system, source timestamp, ingestion timestamp, schema version
- `scenarioFlags`: malformed, partial, retry, duplicate participant id, invalid address
- `retrySimulation`: retryable DynamoDB write-conflict metadata when applicable

Indicative edge cases include:

- Malformed email addresses
- Missing phone numbers
- Invalid addresses with state `ZZ` and malformed ZIP codes
- Duplicate participant ids
- Terminated participants
- Retryable DynamoDB conditional write conflicts

## Load Testing Strategy

Use the generated large payloads as S3 objects and pass S3 pointers through SQS or Step Functions. Do not push these JSON documents directly through SQS.

Recommended financial split:

- One S3 object for the full payload
- One Step Functions map item per `pages[*]`
- One ECS worker task per page
- 250 members per financial page
- Use `subPages` when a page must be split again for replay or targeted retry

Recommended indicative split:

- One S3 object for the full payload
- Chunk `entities` into groups of 500
- Use Step Functions distributed map or ECS fan-out workers
- Reduce to 250 entities per chunk if dependent and beneficiary density increases

Current generated metrics are in `payloads/payload-metrics.json`.

## Regeneration Commands

Run from the repository root on Windows PowerShell:

```powershell
mvn -pl services/common-library -DskipTests package
mvn -pl services/common-library dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
$cp = Get-Content services/common-library/target/classpath.txt
java -cp "services/common-library/target/classes;$cp" com.pearl.common.test.generator.PayloadGeneratorCli "D:\office-work\Empwr\Code\pearl-payroll-platform"
```

The generator writes:

- `payloads/financial/financial-payload-large.json`
- `payloads/financial/scenarios/*.json`
- `payloads/indicative/indicative-payload-large.json`
- `payloads/indicative/scenarios/*.json`
- `payloads/payload-metrics.json`

## Java Test Usage

Use seeded options in unit or integration tests for dynamic but reproducible payloads:

```java
FinancialPayloadGenerator generator = new FinancialPayloadGenerator();
generator.writePayload(
        tempDir.resolve("financial.json"),
        FinancialPayloadOptions.small(91001L).withSize(2, 5));

IndicativePayloadGenerator indicativeGenerator = new IndicativePayloadGenerator();
indicativeGenerator.writePayload(
        tempDir.resolve("indicative.json"),
        IndicativePayloadOptions.small(92001L).withEntityCount(12));
```

For deterministic reruns, keep the same seed. To expand coverage, vary the seed and scenario rates.

## Validation Guidance

These payloads are syntactically valid JSON. Malformed records are intentionally domain-invalid while remaining parseable so downstream systems can test validation, quarantine, retry, and partial-failure handling.

Recommended validation layers:

- JSON parse validation
- Schema validation
- Domain validation
- Idempotency validation
- Chunk split and recomposition validation
- Retry and dead-letter routing validation
- OpenTelemetry trace and correlation id propagation validation
