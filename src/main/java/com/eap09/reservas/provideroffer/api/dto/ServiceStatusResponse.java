package com.eap09.reservas.provideroffer.api.dto;

/**
 * Response DTO para operaciones de cambio de estado de servicio.
 * Devuelve id, nombre y estado actual del servicio modificado.
 */
public record ServiceStatusResponse(
        Long idServicio,
        String nombreServicio,
        String estadoActual
) {
}
