package com.eap09.reservas.administration.api.dto;

public record UserRoleUpdateResponse(
        Long idUsuario,
        String correo,
        String rolAnterior,
        String rolActual
) {
}