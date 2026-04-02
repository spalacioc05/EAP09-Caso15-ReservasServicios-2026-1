package com.eap09.reservas.customerbooking.api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(

        @NotNull(message = "idDisponibilidadServicio es obligatorio")
        Long idDisponibilidadServicio,

        @NotNull(message = "idUsuarioCliente es obligatorio")
        Long idUsuarioCliente

) {
}