package com.pearl.payroll.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialPayrollRecord(
        @NotBlank
        String employeeId,
        @NotNull
        LocalDate payPeriodStart,
        @NotNull
        LocalDate payPeriodEnd,
        @NotNull
        @PositiveOrZero
        BigDecimal grossPay,
        @NotNull
        @PositiveOrZero
        BigDecimal netPay,
        @NotNull
        @Valid
        List<Deduction> deductions,
        @NotNull
        @Valid
        List<TaxDetail> taxes,
        @NotNull
        @Valid
        List<Contribution> contributions) {
}
