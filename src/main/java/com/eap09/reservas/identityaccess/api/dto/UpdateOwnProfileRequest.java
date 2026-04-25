package com.eap09.reservas.identityaccess.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
        @Size(max = 100, message = "nombres debe tener maximo 100 caracteres")
        String nombres,

        @Size(max = 100, message = "apellidos debe tener maximo 100 caracteres")
        String apellidos,

        @Size(max = 120, message = "correo debe tener maximo 120 caracteres")
        String correo
) {
}