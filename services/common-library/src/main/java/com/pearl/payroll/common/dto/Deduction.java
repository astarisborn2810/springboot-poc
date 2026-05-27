package com.pearl.payroll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record Deduction(
        @NotBlank
        String deductionCode,
        String description,
        @NotNull
        @PositiveOrZero
        BigDecimal amount,
        boolean pretax) {
}
