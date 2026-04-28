package com.eap09.reservas.provideroffer.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO para cambio de estado de un servicio propio del proveedor.
 * Soporta transiciones ACTIVO ↔ INACTIVO.
 */
public record ServiceStatusRequest(
        @NotBlank(message = "El estado objetivo del servicio es requerido")
        String targetStatus
) {
}
