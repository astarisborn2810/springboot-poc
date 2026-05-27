package com.pearl.downstream.trigger.idempotency;

public record IdempotencyClaim(
        String idempotencyKey,
        Decision decision,
        String existingStatus) {

    public enum Decision {
        ACQUIRED,
        DUPLICATE_COMPLETED,
        IN_PROGRESS
    }

    public static IdempotencyClaim acquired(String idempotencyKey) {
        return new IdempotencyClaim(idempotencyKey, Decision.ACQUIRED, null);
    }

    public static IdempotencyClaim duplicateCompleted(String idempotencyKey, String existingStatus) {
        return new IdempotencyClaim(idempotencyKey, Decision.DUPLICATE_COMPLETED, existingStatus);
    }

    public static IdempotencyClaim inProgress(String idempotencyKey, String existingStatus) {
        return new IdempotencyClaim(idempotencyKey, Decision.IN_PROGRESS, existingStatus);
    }

    public boolean acquired() {
        return decision == Decision.ACQUIRED;
    }

    public boolean duplicateCompleted() {
        return decision == Decision.DUPLICATE_COMPLETED;
    }

    public boolean inProgress() {
        return decision == Decision.IN_PROGRESS;
    }
}
