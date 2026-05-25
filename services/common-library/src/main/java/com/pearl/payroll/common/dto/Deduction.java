package com.pearl.payroll.common.dto;

import java.math.BigDecimal;

public record Deduction(
        String deductionCode,
        String description,
        BigDecimal amount,
        boolean pretax) {
}
