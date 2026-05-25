package com.pearl.common.dto;

import java.time.Instant;
import java.util.Map;

public record AuditMetadata(
        String createdBy,
        String updatedBy,
        String sourceSystem,
        Instant createdAt,
        Instant updatedAt,
        Map<String, String> attributes) {

    public AuditMetadata {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AuditMetadata system(String sourceSystem) {
        return new AuditMetadata("system", "system", sourceSystem, Instant.now(), Instant.now(), Map.of());
    }
}
