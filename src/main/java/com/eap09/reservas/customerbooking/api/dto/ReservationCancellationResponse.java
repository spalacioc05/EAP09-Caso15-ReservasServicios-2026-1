package com.eap09.reservas.customerbooking.api.dto;

import java.time.OffsetDateTime;

public record ReservationCancellationResponse(
        Long bookingId,
        String bookingStatus,
        OffsetDateTime canceledAt
) {
}