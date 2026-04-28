package com.eap09.reservas.customerbooking.api.dto;

import java.time.OffsetDateTime;

public record ReservationFinalizationResponse(
        Long bookingId,
        String bookingStatus,
        OffsetDateTime finalizedAt
) {
}