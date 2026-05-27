package com.pearl.payroll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BenefitEnrollment(
        @NotBlank
        String benefitCode,
        @NotBlank
        String planName,
        @NotBlank
        String coverageTier,
        @NotNull
        LocalDate effectiveDate,
        LocalDate endDate) {
}
