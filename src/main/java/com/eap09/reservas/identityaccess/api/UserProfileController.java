package com.eap09.reservas.identityaccess.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileRequest;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileResponse;
import com.eap09.reservas.identityaccess.application.UserProfileService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me/profile")
@Tag(name = "Identity Access")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PatchMapping
    @Operation(
            summary = "Actualizar perfil propio del usuario autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido o reglas de validacion incumplidas",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Sin cambios o correo duplicado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<ApiResponse<UpdateOwnProfileResponse>> updateOwnProfile(
            @Valid @RequestBody UpdateOwnProfileRequest request,
            Authentication authentication) {

        Authentication resolvedAuthentication = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        String authenticatedUsername = authenticatedUsername(resolvedAuthentication);
        UpdateOwnProfileResponse response = userProfileService.updateOwnProfile(authenticatedUsername, request);

        ApiResponse<UpdateOwnProfileResponse> body = new ApiResponse<>(
                "Perfil actualizado correctamente",
                response,
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(body);
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }
        return authentication.getName();
    }
}