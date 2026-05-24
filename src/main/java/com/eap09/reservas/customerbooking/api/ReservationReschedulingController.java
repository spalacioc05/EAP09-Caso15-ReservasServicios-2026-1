package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingRequest;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingResponse;
import com.eap09.reservas.customerbooking.application.ReservationReschedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/bookings")
@Tag(name = "Reservation")
public class ReservationReschedulingController {

    private final ReservationReschedulingService reservationReschedulingService;

    public ReservationReschedulingController(ReservationReschedulingService reservationReschedulingService) {
        this.reservationReschedulingService = reservationReschedulingService;
    }

    @PatchMapping("/{bookingId}/reschedule")
    @Operation(
            summary = "Reprogramar una reserva propia del cliente autenticado",
            description = "Actualiza la disponibilidad asociada a una reserva activa del cliente sin crear una nueva reserva ni perder trazabilidad.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserva reprogramada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para cliente propietario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reserva o disponibilidad no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Reserva no reprogramable o disponibilidad destino no valida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al reprogramar reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ReservationReschedulingResponse>>> rescheduleOwnBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody ReservationReschedulingRequest request,
            Authentication authentication) {
        Authentication resolvedAuthentication = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        if (resolvedAuthentication == null
                || resolvedAuthentication.getName() == null
                || resolvedAuthentication.getName().isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        ReservationReschedulingResponse payload = reservationReschedulingService.rescheduleOwnBooking(
                resolvedAuthentication.getName(),
                bookingId,
                request);

        ApiResponse<ReservationReschedulingResponse> response = new ApiResponse<>(
                "Reserva reprogramada correctamente",
                payload,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ReservationReschedulingResponse>> model = EntityModel.of(
                response,
                Link.of(ApiPaths.API_V1 + "/bookings/" + bookingId + "/reschedule").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/bookings/me").withRel("my-bookings"));

        return ResponseEntity.ok(model);
    }
}