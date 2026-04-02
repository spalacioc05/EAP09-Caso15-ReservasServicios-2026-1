package com.eap09.reservas.customerbooking.api.dto;

public record OfferResponse(
        Long idServicio,
        String nombreServicio,
        String descripcion,
        String nombreProveedor
) {
}