package com.eap09.reservas.administration.api;

import com.eap09.reservas.administration.api.dto.OperationalReportResponse;
import com.eap09.reservas.administration.application.OperationalReportAdministrationResult;
import com.eap09.reservas.administration.application.OperationalReportAdministrationService;
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
import java.time.LocalDate;
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
@RequestMapping(ApiPaths.API_V1 + "/admin/reports/operational")
@Tag(name = "Administration")
public class OperationalReportAdministrationController {

    private final OperationalReportAdministrationService operationalReportAdministrationService;

    public OperationalReportAdministrationController(
            OperationalReportAdministrationService operationalReportAdministrationService) {
        this.operationalReportAdministrationService = operationalReportAdministrationService;
    }

    @GetMapping
    @Operation(
            summary = "Generar reporte operativo global de reservas",
            description = "Permite a un administrador autenticado generar un reporte operativo global basado en el historial de reservas, con filtros opcionales por rango de fechas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reporte operativo generado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Filtros invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para administrador autenticado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "No existe historial suficiente para generar el reporte",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al generar el reporte operativo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ApiResponse<OperationalReportResponse>> generateOperationalReport(
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

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                resolvedAuthentication.getName(),
                from,
                to);

        ApiResponse<OperationalReportResponse> response = new ApiResponse<>(
                result.message(),
                result.report(),
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(response);
    }
}