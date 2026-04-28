package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.CustomerReservationResponse;
import java.util.List;

public record CustomerReservationQueryResult(
        String message,
        List<CustomerReservationResponse> bookings
) {
}