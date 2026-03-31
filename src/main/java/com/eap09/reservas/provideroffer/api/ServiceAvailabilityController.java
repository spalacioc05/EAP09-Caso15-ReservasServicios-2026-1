package com.eap09.reservas.provideroffer.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityCreateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityResponse;
import com.eap09.reservas.provideroffer.application.ServiceAvailabilityService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/services/{serviceId}/availabilities")
@Tag(name = "Provider Offer")
public class ServiceAvailabilityController {

    private final ServiceAvailabilityService serviceAvailabilityService;

    public ServiceAvailabilityController(ServiceAvailabilityService serviceAvailabilityService) {
        this.serviceAvailabilityService = serviceAvailabilityService;
    }

    @PostMapping
    @Operation(summary = "Crear franja de disponibilidad para un servicio del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Disponibilidad creada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido o franja no valida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo a proveedor autorizado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Servicio no encontrado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Superposicion detectada",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ServiceAvailabilityResponse>>> createAvailability(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceAvailabilityCreateRequest request,
            Authentication authentication) {

        String username = authenticatedUsername(authentication);
        ServiceAvailabilityResponse response = serviceAvailabilityService.createAvailability(username, serviceId, request);

        ApiResponse<ServiceAvailabilityResponse> payload = new ApiResponse<>(
                "Disponibilidad creada correctamente",
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ServiceAvailabilityResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId + "/availabilities").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId + "/availabilities/"
                        + response.idDisponibilidad() + "/block").withRel("block"),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PatchMapping("/{availabilityId}/block")
    @Operation(summary = "Bloquear franja de disponibilidad de un servicio del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Disponibilidad bloqueada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo a proveedor autorizado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Disponibilidad no encontrada",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<ServiceAvailabilityResponse>>> blockAvailability(
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId,
            Authentication authentication) {

        String username = authenticatedUsername(authentication);
        ServiceAvailabilityResponse response = serviceAvailabilityService.blockAvailability(username, serviceId, availabilityId);

        ApiResponse<ServiceAvailabilityResponse> payload = new ApiResponse<>(
                "Disponibilidad bloqueada correctamente",
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<ServiceAvailabilityResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId + "/availabilities/"
                        + availabilityId + "/block").withSelfRel(),
                Link.of(ApiPaths.API_V1 + "/providers/me/services/" + serviceId + "/availabilities").withRel("service-availabilities"),
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
}