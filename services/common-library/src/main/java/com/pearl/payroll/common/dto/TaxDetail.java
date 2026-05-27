package com.pearl.payroll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record TaxDetail(
        @NotBlank
        String jurisdiction,
        @NotBlank
        String taxType,
        @NotNull
        @PositiveOrZero
        BigDecimal taxableWages,
        @NotNull
        @PositiveOrZero
        BigDecimal withheldAmount) {
}
