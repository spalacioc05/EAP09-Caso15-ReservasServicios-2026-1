package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.ReservationFinalizationResponse;
import com.eap09.reservas.customerbooking.application.ProviderReservationFinalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/bookings")
@Tag(name = "Provider Booking")
public class ProviderReservationFinalizationController {

    private final ProviderReservationFinalizationService providerReservationFinalizationService;

    public ProviderReservationFinalizationController(
            ProviderReservationFinalizationService providerReservationFinalizationService) {
        this.providerReservationFinalizationService = providerReservationFinalizationService;
    }

    @PatchMapping("/{bookingId}/finalization")
    @Operation(summary = "Finalizar una reserva atendida del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserva finalizada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para proveedor propietario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Reserva no finalizable por estado o tiempo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al finalizar reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ReservationFinalizationResponse>>> finalizeOwnBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        String username = authenticatedUsername(authentication);

        ReservationFinalizationResponse payload = providerReservationFinalizationService.finalizeOwnBooking(
                username,
                bookingId);

        ApiResponse<ReservationFinalizationResponse> response = new ApiResponse<>(
                "Reserva finalizada correctamente",
                payload,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ReservationFinalizationResponse>> model = EntityModel.of(
                response,
                Link.of(ApiPaths.API_V1 + "/providers/me/bookings/" + bookingId + "/finalization").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/providers/me/bookings").withRel("provider-bookings"));

        return ResponseEntity.ok(model);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }
}