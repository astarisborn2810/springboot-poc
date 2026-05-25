package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record BatchExecutionRequest(
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        String payloadType,
        String s3Bucket,
        String s3Key,
        Instant requestedAt,
        AuditMetadata auditMetadata,
        Map<String, String> parameters) {

    public BatchExecutionRequest {
        requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("unknown") : auditMetadata;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
