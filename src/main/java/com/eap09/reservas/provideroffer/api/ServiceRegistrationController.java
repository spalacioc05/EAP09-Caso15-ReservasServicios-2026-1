package com.eap09.reservas.provideroffer.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationResponse;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateResponse;
import com.eap09.reservas.provideroffer.application.ServiceRegistrationService;
import com.eap09.reservas.provideroffer.application.ServiceStatusManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/services")
@Tag(name = "Provider Offer")
public class ServiceRegistrationController {

    private final ServiceRegistrationService serviceRegistrationService;
        private final ServiceStatusManagementService serviceStatusManagementService;

        public ServiceRegistrationController(ServiceRegistrationService serviceRegistrationService,
                                                                                 ServiceStatusManagementService serviceStatusManagementService) {
        this.serviceRegistrationService = serviceRegistrationService;
                this.serviceStatusManagementService = serviceStatusManagementService;
    }

    @PostMapping
    @Operation(summary = "Registrar servicio para el proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Servicio registrado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido o reglas de negocio incumplidas",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo a proveedor",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Nombre de servicio duplicado para el proveedor",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ServiceRegistrationResponse>>> registerService(
            @Valid @RequestBody ServiceRegistrationRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        ServiceRegistrationResponse response = serviceRegistrationService.registerService(
                authentication.getName(),
                request);

        ApiResponse<ServiceRegistrationResponse> payload = new ApiResponse<>(
                "Servicio registrado correctamente",
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ServiceRegistrationResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/services").withSelfRel(),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PatchMapping("/{serviceId}/status")
    @Operation(summary = "Activar o inactivar un servicio propio del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado del servicio actualizado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido o estado objetivo no permitido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El servicio no pertenece al proveedor autenticado o el usuario no es proveedor",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Servicio no encontrado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El servicio ya se encuentra en el estado solicitado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "No fue posible completar el cambio de estado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ServiceStatusUpdateResponse>>> updateOwnServiceStatus(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceStatusUpdateRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        ServiceStatusUpdateResponse response = serviceStatusManagementService.updateOwnServiceStatus(
                authentication.getName(),
                serviceId,
                request);

        String normalizedTargetStatus = request.targetStatus().trim().toUpperCase(Locale.ROOT);
        String message = "ACTIVO".equals(normalizedTargetStatus)
                ? "Servicio activado correctamente"
                : "Servicio inactivado correctamente";

        ApiResponse<ServiceStatusUpdateResponse> payload = new ApiResponse<>(
                message,
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ServiceStatusUpdateResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId + "/status").withSelfRel(),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.ok(model);
    }
}