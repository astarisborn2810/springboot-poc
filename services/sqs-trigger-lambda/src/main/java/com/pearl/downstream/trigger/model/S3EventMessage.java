package com.pearl.downstream.trigger.model;

import java.time.Instant;

public record S3EventMessage(
        String bucketName,
        String objectKey,
        String eventType,
        Instant eventTime,
        String awsRegion,
        Long objectSize,
        String eTag,
        String sequencer,
        String sourceMessageId,
        String rawMessageBody,
        CorrelationMetadata correlation) {
}
