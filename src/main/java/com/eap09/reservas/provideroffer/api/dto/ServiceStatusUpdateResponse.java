package com.eap09.reservas.provideroffer.api.dto;

public record ServiceStatusUpdateResponse(
        Long idServicio,
        String nombre,
        String estadoServicio
) {
}