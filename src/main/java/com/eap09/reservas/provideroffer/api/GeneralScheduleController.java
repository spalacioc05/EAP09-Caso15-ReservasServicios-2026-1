package com.eap09.reservas.provideroffer.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleResponse;
import com.eap09.reservas.provideroffer.api.dto.GeneralScheduleUpsertRequest;
import com.eap09.reservas.provideroffer.application.GeneralScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/providers/me/general-schedule")
@Tag(name = "Provider Offer")
public class GeneralScheduleController {

    private final GeneralScheduleService generalScheduleService;

    public GeneralScheduleController(GeneralScheduleService generalScheduleService) {
        this.generalScheduleService = generalScheduleService;
    }

    @PutMapping("/{dayOfWeek}")
    @Operation(
            summary = "Definir o reemplazar horario general semanal del proveedor autenticado",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Horario general definido correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload o rango horario invalido",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo a proveedor",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<GeneralScheduleResponse>>> upsertGeneralSchedule(
            @Parameter(description = "Dia de la semana en mayusculas, por ejemplo LUNES", required = true)
            @PathVariable String dayOfWeek,
            @Valid @RequestBody GeneralScheduleUpsertRequest request,
                        Authentication authentication) {

                if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        GeneralScheduleResponse response = generalScheduleService.upsertGeneralSchedule(
                                authentication.getName(),
                dayOfWeek,
                request);

        ApiResponse<GeneralScheduleResponse> payload = new ApiResponse<>(
                "Horario general definido correctamente",
                response,
                TraceIdUtil.currentTraceId());

        EntityModel<ApiResponse<GeneralScheduleResponse>> model = EntityModel.of(
                payload,
                Link.of(ApiPaths.API_V1 + "/providers/me/general-schedule/" + dayOfWeek).withSelfRel(),
                Link.of(ApiPaths.PROTECTED + "/provider-offer/bootstrap").withRel("provider-offer-bootstrap")
        );

        return ResponseEntity.ok(model);
    }
}
