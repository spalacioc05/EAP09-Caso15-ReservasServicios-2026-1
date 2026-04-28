package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.ProviderBookingResponse;
import java.util.List;

public record ProviderBookingQueryResult(
        String message,
        List<ProviderBookingResponse> bookings
) {
}