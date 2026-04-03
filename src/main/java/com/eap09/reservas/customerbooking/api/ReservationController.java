package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.application.ReservationService;
import com.eap09.reservas.config.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/bookings")
@Tag(name = "Customer Booking")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
        @Operation(summary = "Crear reserva para cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reserva creada correctamente")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Proveedor, servicio y franja son requeridos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para clientes autenticados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entidad relacionada no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Proveedor, servicio o franja no disponibles",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "No fue posible completar la reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<EntityModel<ApiResponse<CreateReservationResponse>>> createReservation(
            @RequestBody CreateReservationRequest request,
            Authentication authentication
    ) {
        String username = authenticatedUsername(authentication);
        CreateReservationResponse response = reservationService.createReservation(request, username);

        ApiResponse<CreateReservationResponse> payload = new ApiResponse<>(
            "Reserva creada correctamente",
            response,
            TraceIdUtil.currentTraceId()
        );

        EntityModel<ApiResponse<CreateReservationResponse>> model = EntityModel.of(
            payload,
            Link.of(ApiPaths.API_V1 + "/bookings").withSelfRel(),
            Link.of(ApiPaths.API_V1 + "/providers/" + response.providerId()
                + "/services/" + response.serviceId()
                + "/availabilities?date=" + response.slotDate()).withRel("availability"),
            Link.of(ApiPaths.API_V1 + "/offers").withRel("offers")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(model);
        }

        private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }
}