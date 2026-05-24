package com.eap09.reservas.administration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRoleUpdateRequest(
        @NotBlank(message = "roleName es obligatorio")
        @Size(max = 50, message = "roleName debe tener maximo 50 caracteres")
        String roleName
) {
}