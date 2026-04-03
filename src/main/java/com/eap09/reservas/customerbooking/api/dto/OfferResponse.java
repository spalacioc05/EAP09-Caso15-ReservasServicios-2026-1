package com.eap09.reservas.customerbooking.api.dto;

public record OfferResponse(
        Long serviceId,
        String serviceName,
        String serviceDescription,
        String providerName
) {
}