package com.eap09.reservas.administration.api.dto;

public record BookingsByProviderResponse(
        Long providerId,
        String providerName,
        long total
) {
}