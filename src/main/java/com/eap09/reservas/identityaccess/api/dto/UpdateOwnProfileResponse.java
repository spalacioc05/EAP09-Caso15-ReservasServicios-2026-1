package com.eap09.reservas.identityaccess.api.dto;

public record UpdateOwnProfileResponse(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo
) {
}