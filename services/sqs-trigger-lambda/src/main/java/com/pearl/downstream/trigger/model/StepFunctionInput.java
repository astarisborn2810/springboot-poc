package com.pearl.downstream.trigger.model;

import java.time.Instant;
import java.util.Map;

public record StepFunctionInput(
        String correlationId,
        String batchId,
        String vendorId,
        String dataType,
        String bucket,
        String key,
        String eventType,
        Instant eventTime,
        String awsRegion,
        Long objectSize,
        String eTag,
        String sequencer,
        String sourceMessageId,
        Map<String, String> metadata) {

    public static StepFunctionInput from(S3EventMessage message) {
        CorrelationMetadata correlation = message.correlation();
        return new StepFunctionInput(
                correlation.correlationId(),
                correlation.batchId(),
                correlation.vendorId(),
                correlation.dataType(),
                message.bucketName(),
                message.objectKey(),
                message.eventType(),
                message.eventTime(),
                message.awsRegion(),
                message.objectSize(),
                message.eTag(),
                message.sequencer(),
                message.sourceMessageId(),
                Map.of(
                        "source", "s3-event-notification",
                        "trigger", "sqs-trigger-lambda"));
    }
}
