package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ReservationReschedulingResponse(
        Long bookingId,
        Long previousAvailabilityId,
        Long newAvailabilityId,
        LocalDate previousDate,
        LocalTime previousStartTime,
        LocalTime previousEndTime,
        LocalDate newDate,
        LocalTime newStartTime,
        LocalTime newEndTime,
        String bookingStatus,
        OffsetDateTime updatedAt
) {
}