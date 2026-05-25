package com.pearl.payroll.common.dto;

import java.math.BigDecimal;

public record Contribution(
        String contributionCode,
        String source,
        BigDecimal amount) {
}
