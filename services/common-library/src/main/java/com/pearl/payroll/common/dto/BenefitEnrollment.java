package com.pearl.payroll.common.dto;

import java.time.LocalDate;

public record BenefitEnrollment(
        String benefitCode,
        String planName,
        String coverageTier,
        LocalDate effectiveDate,
        LocalDate endDate) {
}
