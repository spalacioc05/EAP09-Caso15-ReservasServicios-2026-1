package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ProviderBookingResponse(
        Long bookingId,
        Long serviceId,
        String serviceName,
        Long availabilityId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        Long customerId,
        String customerFullName,
        String customerEmail,
        String bookingStatus,
        OffsetDateTime createdAt
) {
}