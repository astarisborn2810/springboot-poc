package com.pearl.common.dto;

public record ServiceStatusResponse(
        String service,
        String capability,
        String status) {
}
