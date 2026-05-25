package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record BaseEvent(
        String eventId,
        String eventType,
        String correlationId,
        String batchId,
        String vendorId,
        String planId,
        String executionId,
        String requestId,
        Instant occurredAt,
        AuditMetadata auditMetadata,
        Map<String, String> attributes) {

    public BaseEvent {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        auditMetadata = auditMetadata == null ? AuditMetadata.system("unknown") : auditMetadata;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
