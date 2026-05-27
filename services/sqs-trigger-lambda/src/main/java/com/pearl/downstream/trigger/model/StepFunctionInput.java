package com.pearl.downstream.trigger.model;

import java.time.Instant;
import java.util.Map;

public record StepFunctionInput(
        String correlationId,
        String batchId,
        String vendorId,
        String dataType,
        String payloadType,
        String fileName,
        String s3PathOrArn,
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
                correlation.dataType(),
                fileName(message.objectKey()),
                s3Path(message.bucketName(), message.objectKey()),
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

    private static String fileName(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String normalized = objectKey.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String lastSegment = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        int extensionIndex = lastSegment.lastIndexOf('.');
        return extensionIndex > 0 ? lastSegment.substring(0, extensionIndex) : lastSegment;
    }

    private static String s3Path(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank() || objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return "s3://" + bucket + "/" + objectKey;
    }
}
