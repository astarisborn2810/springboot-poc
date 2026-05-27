package com.pearl.payroll.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record IndicativeEmployee(
        @NotBlank
        String employeeId,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotNull
        LocalDate dateOfBirth,
        @NotBlank
        String homeState,
        @NotNull
        @Valid
        ParticipantDetail participant,
        @NotNull
        @Valid
        List<BenefitEnrollment> benefits) {
}
