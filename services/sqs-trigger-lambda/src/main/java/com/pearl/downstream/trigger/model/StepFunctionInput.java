package com.pearl.downstream.trigger.model;

import com.pearl.downstream.trigger.util.IdempotencyKeyUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
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
        String idempotencyKey = IdempotencyKeyUtil.forEvent(message);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "s3-event-notification");
        metadata.put("trigger", "sqs-trigger-lambda");
        metadata.put("idempotencyKey", idempotencyKey);
        putIfPresent(metadata, "eTag", message.eTag());
        putIfPresent(metadata, "sequencer", message.sequencer());
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
                Map.copyOf(metadata));
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

    private static void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
