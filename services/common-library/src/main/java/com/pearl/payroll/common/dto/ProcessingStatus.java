package com.pearl.payroll.common.dto;

public enum ProcessingStatus {
    RECEIVED,
    VALIDATING,
    ROUTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REJECTED
}
