package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record CustomerReservationResponse(
        Long bookingId,
        Long serviceId,
        String serviceName,
        Long providerId,
        String providerFullName,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String bookingStatus,
        OffsetDateTime createdAt
) {
}