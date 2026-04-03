package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CreateReservationResponse(

        Long bookingId,
        Long providerId,
        Long serviceId,
        Long availabilityId,
        Long customerId,
        LocalDate slotDate,
        String bookingStatus,
        OffsetDateTime createdAt

) {
}