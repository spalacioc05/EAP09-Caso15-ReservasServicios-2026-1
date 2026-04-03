package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import java.util.List;

public record AvailabilityQueryResult(
        String message,
        List<AvailabilityResponse> availabilities
) {
}
