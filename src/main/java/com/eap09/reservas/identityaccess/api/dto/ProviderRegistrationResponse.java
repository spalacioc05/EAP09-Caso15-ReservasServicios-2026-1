package com.eap09.reservas.identityaccess.api.dto;

public record ProviderRegistrationResponse(
        Long idUsuario,
        String correo,
        String rol,
        String estado
) {
}
