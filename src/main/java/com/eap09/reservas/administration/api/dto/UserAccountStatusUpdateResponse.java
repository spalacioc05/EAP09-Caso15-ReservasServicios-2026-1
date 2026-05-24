package com.eap09.reservas.administration.api.dto;

public record UserAccountStatusUpdateResponse(
        Long idUsuario,
        String correo,
        String estadoAnterior,
        String estadoActual
) {
}