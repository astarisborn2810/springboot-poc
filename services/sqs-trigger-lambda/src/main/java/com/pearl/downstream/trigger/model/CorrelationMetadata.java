package com.pearl.downstream.trigger.model;

public record CorrelationMetadata(
        String correlationId,
        String batchId,
        String vendorId,
        String dataType) {
}
