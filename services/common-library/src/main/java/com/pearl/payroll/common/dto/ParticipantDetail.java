package com.pearl.payroll.common.dto;

import java.time.LocalDate;

public record ParticipantDetail(
        String participantId,
        String employeeId,
        String employmentStatus,
        LocalDate hireDate,
        LocalDate terminationDate) {
}
