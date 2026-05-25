package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        String errorCode,
        String message,
        boolean retryable,
        Instant timestamp,
        AuditMetadata auditMetadata,
        Map<String, String> metadata) {

    public ErrorResponse {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("unknown") : auditMetadata;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
