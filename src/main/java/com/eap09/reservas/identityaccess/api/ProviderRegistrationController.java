package com.eap09.reservas.identityaccess.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationRequest;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationResponse;
import com.eap09.reservas.identityaccess.application.ProviderRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers")
@Tag(name = "Identity Access")
public class ProviderRegistrationController {

    private final ProviderRegistrationService providerRegistrationService;

    public ProviderRegistrationController(ProviderRegistrationService providerRegistrationService) {
        this.providerRegistrationService = providerRegistrationService;
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva cuenta de proveedor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cuenta de proveedor creada")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload de registro invalido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Correo ya registrado",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<ApiResponse<ProviderRegistrationResponse>> registerProvider(
            @Valid @RequestBody ProviderRegistrationRequest request) {

        ProviderRegistrationResponse response = providerRegistrationService.registerProvider(request);
        ApiResponse<ProviderRegistrationResponse> body = new ApiResponse<>(
                "Proveedor registrado correctamente",
                response,
                TraceIdUtil.currentTraceId());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
