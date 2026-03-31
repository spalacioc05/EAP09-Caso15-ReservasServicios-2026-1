package com.eap09.reservas.identityaccess.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderRegistrationRequest(
        @NotBlank(message = "nombres es obligatorio")
        @Size(max = 100, message = "nombres debe tener maximo 100 caracteres")
        String nombres,

        @NotBlank(message = "apellidos es obligatorio")
        @Size(max = 100, message = "apellidos debe tener maximo 100 caracteres")
        String apellidos,

        @NotBlank(message = "correo es obligatorio")
        @Email(message = "correo debe tener un formato valido")
        @Size(max = 120, message = "correo debe tener maximo 120 caracteres")
        String correo,

        @NotBlank(message = "contrasena es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$",
                message = "contrasena debe tener entre 8 y 64 caracteres e incluir mayuscula, minuscula, numero y caracter especial"
        )
        String contrasena
) {
}
