package com.eap09.reservas.customerbooking.api.dto;

import java.time.OffsetDateTime;

public record CreateReservationResponse(

        Long idReserva,
        Long idDisponibilidadServicio,
        OffsetDateTime fechaCreacionReserva,
        String mensaje

) {
}