package com.eap09.reservas.provideroffer.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record GeneralScheduleUpsertRequest(
        @NotNull(message = "horaInicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "horaFin es obligatoria")
        LocalTime horaFin
) {
}
