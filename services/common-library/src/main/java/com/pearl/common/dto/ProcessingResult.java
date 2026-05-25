package com.pearl.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessingResult(
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        String status,
        int recordsProcessed,
        int recordsSucceeded,
        int recordsFailed,
        List<String> warnings,
        Instant processedAt,
        AuditMetadata auditMetadata,
        Map<String, String> attributes) {

    public ProcessingResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        processedAt = processedAt == null ? Instant.now() : processedAt;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("unknown") : auditMetadata;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
