package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record S3PayloadMetadata(
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        String bucket,
        String key,
        String eTag,
        long contentLength,
        String contentType,
        Instant lastModifiedAt,
        Instant capturedAt,
        AuditMetadata auditMetadata,
        Map<String, String> userMetadata) {

    public S3PayloadMetadata {
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("s3") : auditMetadata;
        userMetadata = userMetadata == null ? Map.of() : Map.copyOf(userMetadata);
    }
}
