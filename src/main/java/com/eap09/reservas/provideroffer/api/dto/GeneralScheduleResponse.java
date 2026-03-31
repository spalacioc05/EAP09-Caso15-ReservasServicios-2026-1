package com.eap09.reservas.provideroffer.api.dto;

import java.time.LocalTime;

public record GeneralScheduleResponse(
        Long providerUserId,
        String dayOfWeek,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
