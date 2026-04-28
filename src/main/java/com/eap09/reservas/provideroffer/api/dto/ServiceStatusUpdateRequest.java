package com.eap09.reservas.provideroffer.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceStatusUpdateRequest(
        @NotBlank(message = "targetStatus es obligatorio")
        String targetStatus
) {
}