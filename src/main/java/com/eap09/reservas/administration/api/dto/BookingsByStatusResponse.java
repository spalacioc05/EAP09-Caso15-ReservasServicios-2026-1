package com.eap09.reservas.administration.api.dto;

public record BookingsByStatusResponse(
        String status,
        long total
) {
}