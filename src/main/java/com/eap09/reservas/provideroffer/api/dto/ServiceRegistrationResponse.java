package com.eap09.reservas.provideroffer.api.dto;

public record ServiceRegistrationResponse(
        Long idServicio,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        Integer capacidadMaximaConcurrente,
        String estadoServicio
) {
}