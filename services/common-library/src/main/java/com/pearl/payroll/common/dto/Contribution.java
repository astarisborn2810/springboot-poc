package com.pearl.payroll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record Contribution(
        @NotBlank
        String contributionCode,
        @NotBlank
        String source,
        @NotNull
        @PositiveOrZero
        BigDecimal amount) {
}
