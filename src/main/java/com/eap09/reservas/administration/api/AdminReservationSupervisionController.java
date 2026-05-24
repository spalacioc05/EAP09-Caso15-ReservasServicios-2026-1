package com.eap09.reservas.administration.api;

import com.eap09.reservas.administration.api.dto.AdminReservationSupervisionItemResponse;
import com.eap09.reservas.administration.application.AdminReservationSupervisionResult;
import com.eap09.reservas.administration.application.AdminReservationSupervisionService;
import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.response.ErrorResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(ApiPaths.API_V1 + "/admin/bookings")
@Tag(name = "Administration")
public class AdminReservationSupervisionController {

    private final AdminReservationSupervisionService adminReservationSupervisionService;

    public AdminReservationSupervisionController(AdminReservationSupervisionService adminReservationSupervisionService) {
        this.adminReservationSupervisionService = adminReservationSupervisionService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar reservas globales para supervision administrativa",
            description = "Permite a un administrador autenticado consultar reservas activas e historicas con filtros opcionales por cliente, proveedor, servicio, estado y rango de fechas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta administrativa de reservas procesada")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Filtros invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para administrador autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al consultar reservas administrativas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ApiResponse<List<AdminReservationSupervisionItemResponse>>> getReservations(
            @Parameter(description = "Filtra por id del cliente propietario de la reserva")
            @RequestParam(value = "customerId", required = false)
            @Positive(message = "customerId debe ser positivo") Long customerId,
            @Parameter(description = "Filtra por id del proveedor asociado al servicio reservado")
            @RequestParam(value = "providerId", required = false)
            @Positive(message = "providerId debe ser positivo") Long providerId,
            @Parameter(description = "Filtra por id del servicio reservado")
            @RequestParam(value = "serviceId", required = false)
            @Positive(message = "serviceId debe ser positivo") Long serviceId,
            @Parameter(description = "Filtra por estado de reserva, por ejemplo CREADA, CANCELADA o FINALIZADA")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Fecha inicial del rango de disponibilidad en formato yyyy-MM-dd")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final del rango de disponibilidad en formato yyyy-MM-dd")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        Authentication resolvedAuthentication = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        if (resolvedAuthentication == null
                || resolvedAuthentication.getName() == null
                || resolvedAuthentication.getName().isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                resolvedAuthentication.getName(),
                customerId,
                providerId,
                serviceId,
                status,
                from,
                to);

        ApiResponse<List<AdminReservationSupervisionItemResponse>> response = new ApiResponse<>(
                result.message(),
                result.bookings(),
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(response);
    }
}