package com.pearl.payroll.common.dto;

import java.math.BigDecimal;

public record TaxDetail(
        String jurisdiction,
        String taxType,
        BigDecimal taxableWages,
        BigDecimal withheldAmount) {
}
