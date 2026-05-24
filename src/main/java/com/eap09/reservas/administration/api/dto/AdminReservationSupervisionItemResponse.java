package com.eap09.reservas.administration.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AdminReservationSupervisionItemResponse(
        Long bookingId,
        Long customerId,
        String customerFullName,
        String customerEmail,
        Long providerId,
        String providerFullName,
        String providerEmail,
        Long serviceId,
        String serviceName,
        Long availabilityId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String bookingStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime finishedAt
) {
}