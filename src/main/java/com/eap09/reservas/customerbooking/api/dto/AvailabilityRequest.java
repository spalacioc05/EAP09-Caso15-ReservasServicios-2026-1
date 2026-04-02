package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalDate;

public record AvailabilityRequest(
        Long idServicio,
        LocalDate fecha
) {}