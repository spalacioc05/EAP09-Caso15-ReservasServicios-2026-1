package com.eap09.reservas.identityaccess.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationRequest;
import com.eap09.reservas.identityaccess.api.dto.AuthenticationResponse;
import com.eap09.reservas.identityaccess.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH + "/sessions")
@Tag(name = "Identity Access")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping
    @Operation(summary = "Autenticar usuario con correo y contrasena")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticacion exitosa")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload invalido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales no validas",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cuenta restringida o inactiva",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createSession(
            @Valid @RequestBody AuthenticationRequest request) {

        AuthenticationResponse response = authenticationService.createSession(request);
        ApiResponse<AuthenticationResponse> body = new ApiResponse<>(
                "Autenticacion exitosa",
                response,
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/current")
    @Operation(
            summary = "Cerrar de forma segura la sesion actual",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesion cerrada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autenticacion requerida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "No existe una sesion activa valida",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<ApiResponse<String>> closeCurrentSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            Authentication authentication) {

        Authentication resolvedAuthentication = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();

        if (resolvedAuthentication == null || resolvedAuthentication.getName() == null) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        String token = extractBearerToken(authorizationHeader);
        authenticationService.closeCurrentSession(token, resolvedAuthentication.getName());

        ApiResponse<String> body = new ApiResponse<>(
                "Sesion cerrada correctamente",
                "CERRADA",
                TraceIdUtil.currentTraceId());

        return ResponseEntity.ok(body);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }
        return authorizationHeader.substring(7);
    }
}
