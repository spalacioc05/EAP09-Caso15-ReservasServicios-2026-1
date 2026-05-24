package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.AdminReservationSupervisionItemResponse;
import java.util.List;

public record AdminReservationSupervisionResult(
        String message,
        List<AdminReservationSupervisionItemResponse> bookings
) {
}