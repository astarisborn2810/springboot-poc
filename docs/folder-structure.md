# Folder Structure

```text
pearl-payroll-platform
|-- .github
|   `-- workflows
|-- .mvn
|-- Jenkins
|-- docs
|-- infrastructure
|   |-- cloudformation
|   |-- dynamodb
|   |-- ecs
|   |-- iam
|   |-- lambda
|   |-- s3
|   `-- sqs
|-- local
|   |-- docker-compose
|   |-- localstack
|   |   `-- init
|   `-- postgres
|       `-- init
|-- payloads
|   |-- financial
|   `-- indicative
|-- scripts
|-- services
|   |-- batch-completion-service
|   |-- common-library
|   |-- config-service
|   |-- datatype-router-lambda
|   |-- financial-processing-service
|   |-- indicative-processing-service
|   |-- result-tracking-service
|   `-- sqs-trigger-lambda
|-- step-functions
|-- pom.xml
|-- README.md
`-- .gitignore
```

Each Maven module owns its `pom.xml`, source tree, tests, resources, and deployable artifact definition. Shared code belongs in `services/common-library` only when it is genuinely cross-service and stable.
