package com.pearl.payroll.common.dto;

import java.time.LocalDate;
import java.util.List;

public record IndicativeEmployee(
        String employeeId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String homeState,
        ParticipantDetail participant,
        List<BenefitEnrollment> benefits) {
}
