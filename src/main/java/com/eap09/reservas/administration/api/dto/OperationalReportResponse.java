package com.eap09.reservas.administration.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationalReportResponse(
        LocalDate from,
        LocalDate to,
        long totalBookings,
        long activeBookings,
        long completedBookings,
        long cancelledBookings,
        BigDecimal cancellationRate,
        BigDecimal completionRate,
        BigDecimal occupancyRate,
        List<BookingsByStatusResponse> bookingsByStatus,
        List<BookingsByServiceResponse> bookingsByService,
        List<BookingsByProviderResponse> bookingsByProvider
) {
}