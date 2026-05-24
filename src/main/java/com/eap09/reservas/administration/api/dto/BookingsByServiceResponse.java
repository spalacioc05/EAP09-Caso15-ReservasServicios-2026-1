package com.eap09.reservas.administration.api.dto;

public record BookingsByServiceResponse(
        Long serviceId,
        String serviceName,
        long total
) {
}