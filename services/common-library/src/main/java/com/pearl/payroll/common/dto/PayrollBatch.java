package com.pearl.payroll.common.dto;

import java.time.Instant;
import java.util.Map;

public record PayrollBatch(
        String batchId,
        String tenantId,
        String sourceSystem,
        String payloadType,
        ProcessingStatus status,
        Instant receivedAt,
        Map<String, String> attributes) {
}
