package com.eap09.reservas.customerbooking.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationReschedulingRequest(
        @NotNull(message = "availabilityId es obligatoria")
        Long availabilityId
) {
}