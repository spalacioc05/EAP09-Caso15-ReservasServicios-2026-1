package com.eap09.reservas.administration.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UserAccountStatusUpdateRequest(
        @NotBlank(message = "targetStatus es obligatorio")
        String targetStatus
) {
}