package com.eap09.reservas.provideroffer.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusResponse;
import com.eap09.reservas.provideroffer.application.ServiceStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para cambio de estado de servicios propios del proveedor.
 * 
 * Endpoint: PATCH /api/v1/providers/me/services/{serviceId}/status
 * 
 * Responsabilidades:
 * - Extraer usuario autenticado desde Authentication
 * - Validar presencia de autenticación
 * - Delegar cambio de estado al service
 * - Construir respuesta con HATEOAS
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/services")
@Tag(name = "Provider Offer")
public class ServiceStatusController {

    private final ServiceStatusService serviceStatusService;

    public ServiceStatusController(ServiceStatusService serviceStatusService) {
        this.serviceStatusService = serviceStatusService;
    }

    /**
     * Cambia el estado de un servicio propio (activar o inactivar).
     * 
     * @param serviceId ID del servicio a cambiar de estado
     * @param request body con targetStatus (ACTIVO | INACTIVO)
     * @param authentication objeto con datos del usuario autenticado
     * @return 200 OK con detalles del servicio y nuevo estado
     * 
     * @throws AccessDeniedException si no hay autenticación válida
     */
    @PatchMapping("/{serviceId}/status")
    @Operation(summary = "Cambiar estado de un servicio propio",
            description = "Activa o inactiva un servicio propio. " +
                    "Un servicio inactivo no estará disponible para nuevas reservas. " +
                    "Las reservas ya creadas no se ven afectadas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Estado del servicio cambiado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Payload inválido o estado objetivo inválido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Autenticación requerida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Acceso permitido solo a proveedor, o servicio no es propio",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Servicio no encontrado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ServiceStatusResponse>>> updateServiceStatus(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceStatusRequest request,
            Authentication authentication) {

        // Validar autenticación
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        // Delegar al service
        ServiceStatusResponse response = serviceStatusService.updateServiceStatus(
                authentication.getName(),
                serviceId,
                request);

        // Construir respuesta
        ApiResponse<ServiceStatusResponse> payload = new ApiResponse<>(
                "El estado del servicio ha sido actualizado correctamente",
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ServiceStatusResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId).withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/providers/me/services").withRel("services-list"),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.ok(model);
    }
}
