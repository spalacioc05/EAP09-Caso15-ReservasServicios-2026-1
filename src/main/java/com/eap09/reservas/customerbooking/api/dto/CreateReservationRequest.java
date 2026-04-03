package com.eap09.reservas.customerbooking.api.dto;

public record CreateReservationRequest(

        Long providerId,
        Long serviceId,
        Long availabilityId

) {
}