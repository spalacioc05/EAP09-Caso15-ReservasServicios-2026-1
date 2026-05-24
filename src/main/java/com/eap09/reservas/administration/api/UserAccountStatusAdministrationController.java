package com.eap09.reservas.administration.api;

import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateRequest;
import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateResponse;
import com.eap09.reservas.administration.application.UserAccountStatusAdministrationService;
import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/users")
@Tag(name = "Administration")
public class UserAccountStatusAdministrationController {

    private static final String ACTIVE_STATE = "ACTIVA";

    private final UserAccountStatusAdministrationService userAccountStatusAdministrationService;

    public UserAccountStatusAdministrationController(
            UserAccountStatusAdministrationService userAccountStatusAdministrationService) {
        this.userAccountStatusAdministrationService = userAccountStatusAdministrationService;
    }

    @PatchMapping("/{userId}/status")
    @Operation(
            summary = "Activar o inactivar la cuenta de un usuario registrado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado de cuenta actualizado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido o estado no valido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Usuario autenticado sin privilegios de administrador",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario objetivo no encontrado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "La cuenta ya se encuentra en el estado solicitado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "No fue posible completar la actualizacion del estado de la cuenta",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<ApiResponse<UserAccountStatusUpdateResponse>> updateUserAccountStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserAccountStatusUpdateRequest request,
            Authentication authentication) {

        Authentication resolvedAuthentication = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        if (resolvedAuthentication == null
                || resolvedAuthentication.getName() == null
                || resolvedAuthentication.getName().isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountStatusUpdateResponse response = userAccountStatusAdministrationService.updateUserAccountStatus(
                resolvedAuthentication.getName(),
                userId,
                request);

        String message = ACTIVE_STATE.equals(response.estadoActual())
                ? "Cuenta de usuario activada correctamente"
                : "Cuenta de usuario inactivada correctamente";

        ApiResponse<UserAccountStatusUpdateResponse> body = new ApiResponse<>(
                message,
                response,
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(body);
    }
}