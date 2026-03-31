package com.eap09.reservas.identityaccess.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
        @NotBlank(message = "correo es obligatorio")
        @Email(message = "correo debe tener un formato valido")
        String correo,

        @NotBlank(message = "contrasena es obligatoria")
        String contrasena
) {
}
