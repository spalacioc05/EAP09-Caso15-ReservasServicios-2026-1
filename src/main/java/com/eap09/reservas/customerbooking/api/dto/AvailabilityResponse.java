package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalTime;

public record AvailabilityResponse(
        Long availabilityId,
        LocalTime startTime,
        LocalTime endTime,
        long remainingSlots
) {
}