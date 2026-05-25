package com.pearl.payroll.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialPayrollRecord(
        String employeeId,
        LocalDate payPeriodStart,
        LocalDate payPeriodEnd,
        BigDecimal grossPay,
        BigDecimal netPay,
        List<Deduction> deductions,
        List<TaxDetail> taxes,
        List<Contribution> contributions) {
}
