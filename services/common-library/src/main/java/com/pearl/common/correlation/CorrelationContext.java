package com.pearl.common.correlation;

import java.util.Objects;
import java.util.Optional;

public final class CorrelationContext {

    private static final CorrelationContext EMPTY = builder().build();

    private final String correlationId;
    private final String batchId;
    private final String vendorId;
    private final String planId;
    private final String executionId;
    private final String requestId;

    private CorrelationContext(Builder builder) {
        this.correlationId = normalize(builder.correlationId);
        this.batchId = normalize(builder.batchId);
        this.vendorId = normalize(builder.vendorId);
        this.planId = normalize(builder.planId);
        this.executionId = normalize(builder.executionId);
        this.requestId = normalize(builder.requestId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CorrelationContext empty() {
        return EMPTY;
    }

    public Builder toBuilder() {
        return builder()
                .correlationId(correlationId)
                .batchId(batchId)
                .vendorId(vendorId)
                .planId(planId)
                .executionId(executionId)
                .requestId(requestId);
    }

    public Optional<String> correlationId() {
        return Optional.ofNullable(correlationId);
    }

    public Optional<String> batchId() {
        return Optional.ofNullable(batchId);
    }

    public Optional<String> vendorId() {
        return Optional.ofNullable(vendorId);
    }

    public Optional<String> planId() {
        return Optional.ofNullable(planId);
    }

    public Optional<String> executionId() {
        return Optional.ofNullable(executionId);
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    public boolean isEmpty() {
        return correlationId == null
                && batchId == null
                && vendorId == null
                && planId == null
                && executionId == null
                && requestId == null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CorrelationContext that)) {
            return false;
        }
        return Objects.equals(correlationId, that.correlationId)
                && Objects.equals(batchId, that.batchId)
                && Objects.equals(vendorId, that.vendorId)
                && Objects.equals(planId, that.planId)
                && Objects.equals(executionId, that.executionId)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, batchId, vendorId, planId, executionId, requestId);
    }

    @Override
    public String toString() {
        return "CorrelationContext{"
                + "correlationId='" + correlationId + '\''
                + ", batchId='" + batchId + '\''
                + ", vendorId='" + vendorId + '\''
                + ", planId='" + planId + '\''
                + ", executionId='" + executionId + '\''
                + ", requestId='" + requestId + '\''
                + '}';
    }

    public static final class Builder {
        private String correlationId;
        private String batchId;
        private String vendorId;
        private String planId;
        private String executionId;
        private String requestId;

        private Builder() {
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder vendorId(String vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public Builder planId(String planId) {
            this.planId = planId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public CorrelationContext build() {
            return new CorrelationContext(this);
        }
    }
}
