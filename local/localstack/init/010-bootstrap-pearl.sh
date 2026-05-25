#!/usr/bin/env bash
set -euo pipefail

awslocal s3 mb s3://pearl-payroll-payloads-local || true
awslocal s3 mb s3://pearl-payroll-config-local || true
awslocal s3 mb s3://pearl-payroll-audit-local || true

awslocal sqs create-queue --queue-name pearl-batch-events-local || true
awslocal sqs create-queue --queue-name pearl-completion-events-local || true
awslocal sqs create-queue --queue-name pearl-dead-letter-local || true

awslocal dynamodb create-table \
  --table-name pearl-batch-status-local \
  --attribute-definitions AttributeName=batchId,AttributeType=S \
  --key-schema AttributeName=batchId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST || true

awslocal dynamodb create-table \
  --table-name pearl-participant-status-local \
  --attribute-definitions AttributeName=participantId,AttributeType=S \
  --key-schema AttributeName=participantId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST || true
