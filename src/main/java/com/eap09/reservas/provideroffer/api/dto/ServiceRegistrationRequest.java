package com.eap09.reservas.provideroffer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceRegistrationRequest(
        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotBlank(message = "descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "duracionMinutos es obligatoria")
        @Positive(message = "duracionMinutos debe ser mayor a cero")
        Integer duracionMinutos,

        @NotNull(message = "capacidadMaximaConcurrente es obligatoria")
        @Positive(message = "capacidadMaximaConcurrente debe ser mayor a cero")
        Integer capacidadMaximaConcurrente
) {
}