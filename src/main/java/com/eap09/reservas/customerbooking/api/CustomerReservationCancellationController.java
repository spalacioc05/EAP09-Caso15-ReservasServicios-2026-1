package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.ReservationCancellationResponse;
import com.eap09.reservas.customerbooking.application.CustomerReservationCancellationService;
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
@RequestMapping(ApiPaths.API_V1 + "/bookings")
@Tag(name = "Reservation")
public class CustomerReservationCancellationController {

    private final CustomerReservationCancellationService customerReservationCancellationService;

    public CustomerReservationCancellationController(
            CustomerReservationCancellationService customerReservationCancellationService) {
        this.customerReservationCancellationService = customerReservationCancellationService;
    }

    @PatchMapping("/{bookingId}/cancellation")
    @Operation(summary = "Cancelar una reserva propia del cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserva cancelada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para cliente propietario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Reserva no cancelable por estado o tiempo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al cancelar reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ReservationCancellationResponse>>> cancelOwnBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        String username = authenticatedUsername(authentication);

        ReservationCancellationResponse payload = customerReservationCancellationService.cancelOwnBooking(
                username,
                bookingId);

        ApiResponse<ReservationCancellationResponse> response = new ApiResponse<>(
                "Reserva cancelada correctamente",
                payload,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ReservationCancellationResponse>> model = EntityModel.of(
                response,
                Link.of(ApiPaths.API_V1 + "/bookings/" + bookingId + "/cancellation").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/bookings/me").withRel("my-bookings"));

        return ResponseEntity.ok(model);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }
}