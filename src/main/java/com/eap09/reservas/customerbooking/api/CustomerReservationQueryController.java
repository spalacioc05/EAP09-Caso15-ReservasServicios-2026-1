package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.CustomerReservationResponse;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/bookings/me")
@Tag(name = "Reservation")
public class CustomerReservationQueryController {

    private final CustomerReservationQueryService customerReservationQueryService;

    public CustomerReservationQueryController(CustomerReservationQueryService customerReservationQueryService) {
        this.customerReservationQueryService = customerReservationQueryService;
    }

    @GetMapping
    @Operation(summary = "Consultar reservas del cliente autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta de reservas procesada")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para cliente autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al consultar reservas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<List<CustomerReservationResponse>>>> getOwnBookings(
            Authentication authentication) {
        String username = authenticatedUsername(authentication);

        CustomerReservationQueryResult result = customerReservationQueryService.getOwnBookings(username);

        ApiResponse<List<CustomerReservationResponse>> response = new ApiResponse<>(
                result.message(),
                result.bookings(),
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<List<CustomerReservationResponse>>> model = EntityModel.of(
                response,
                Link.of(ApiPaths.API_V1 + "/bookings/me").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/bookings").withRel("create-booking"));

        return ResponseEntity.ok(model);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }
}