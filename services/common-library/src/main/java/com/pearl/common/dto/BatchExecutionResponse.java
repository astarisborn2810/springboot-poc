package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record BatchExecutionResponse(
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        String status,
        String message,
        Instant startedAt,
        Instant completedAt,
        AuditMetadata auditMetadata,
        Map<String, String> attributes) {

    public BatchExecutionResponse {
        startedAt = startedAt == null ? Instant.now() : startedAt;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("unknown") : auditMetadata;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
