package com.pearl.payroll.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ParticipantDetail(
        @NotBlank
        String participantId,
        @NotBlank
        String employeeId,
        @NotBlank
        String employmentStatus,
        @NotNull
        LocalDate hireDate,
        LocalDate terminationDate) {
}
