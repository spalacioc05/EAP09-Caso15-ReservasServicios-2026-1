package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.application.AvailabilityQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerBookingAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/{providerId}/services/{serviceId}/availabilities")
@Tag(name = "Customer Booking")
public class CustomerBookingAvailabilityController {

    private final CustomerBookingAvailabilityService service;

    public CustomerBookingAvailabilityController(CustomerBookingAvailabilityService service) {
        this.service = service;
    }

        @GetMapping
        @Operation(summary = "Consultar horarios y cupos disponibles para una fecha especifica",
            security = @SecurityRequirement(name = "bearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta de horarios y cupos procesada")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Proveedor, servicio y fecha son requeridos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para clientes autenticados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al consultar disponibilidad",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<EntityModel<ApiResponse<List<AvailabilityResponse>>>> getAvailability(
            @PathVariable Long providerId,
            @PathVariable Long serviceId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        String username = authenticatedUsername(authentication);
        AvailabilityQueryResult result = service.getAvailability(providerId, serviceId, date, username);

        ApiResponse<List<AvailabilityResponse>> response = new ApiResponse<>(
            result.message(),
            result.availabilities(),
                TraceIdUtil.currentTraceId()
        );

        String selfHref = ApiPaths.API_V1 + "/providers/" + providerId + "/services/" + serviceId
            + "/availabilities?date=" + date;

        EntityModel<ApiResponse<List<AvailabilityResponse>>> model = EntityModel.of(
            response,
            Link.of(selfHref).withSelfRel(),
            Link.of(ApiPaths.API_V1 + "/offers").withRel("offers")
        );

        return ResponseEntity.ok(model);
        }

        private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }
}