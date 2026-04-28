package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.ProviderBookingResponse;
import com.eap09.reservas.customerbooking.application.ProviderBookingQueryResult;
import com.eap09.reservas.customerbooking.application.ProviderBookingQueryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/bookings")
@Tag(name = "Provider Booking")
public class ProviderBookingQueryController {

    private final ProviderBookingQueryService providerBookingQueryService;

    public ProviderBookingQueryController(ProviderBookingQueryService providerBookingQueryService) {
        this.providerBookingQueryService = providerBookingQueryService;
    }

    @GetMapping
    @Operation(summary = "Consultar reservas operativas del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta operativa procesada")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Parametros de consulta invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para proveedor autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Servicio filtrado no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al consultar reservas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<List<ProviderBookingResponse>>>> getOwnBookings(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            Authentication authentication) {
        String username = authenticatedUsername(authentication);

        ProviderBookingQueryResult result = providerBookingQueryService.getOwnBookings(
                username,
                date,
                status,
                serviceId);

        ApiResponse<List<ProviderBookingResponse>> response = new ApiResponse<>(
                result.message(),
                result.bookings(),
                TraceIdUtil.currentTraceId());

        String selfHref = buildSelfHref(date, status, serviceId);

        EntityModel<ApiResponse<List<ProviderBookingResponse>>> model = EntityModel.of(
                response,
                Link.of(selfHref).withSelfRel(),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.ok(model);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        return authentication.getName();
    }

    private String buildSelfHref(LocalDate date, String status, Long serviceId) {
        StringBuilder builder = new StringBuilder(ApiPaths.API_V1 + "/providers/me/bookings");
        String separator = "?";

        if (date != null) {
            builder.append(separator).append("date=").append(date);
            separator = "&";
        }

        if (status != null && !status.isBlank()) {
            builder.append(separator).append("status=").append(status.trim());
            separator = "&";
        }

        if (serviceId != null) {
            builder.append(separator).append("serviceId=").append(serviceId);
        }

        return builder.toString();
    }
}