package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.application.CustomerBookingOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/offers")
@Tag(name = "Customer Booking")
public class CustomerBookingController {

    private final CustomerBookingOfferService service;

    public CustomerBookingController(CustomerBookingOfferService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar oferta disponible para clientes autenticados",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Oferta consultada correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso permitido solo para clientes autenticados",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno al consultar la oferta",
            content = @Content(schema = @Schema(implementation = com.eap09.reservas.common.response.ErrorResponse.class)))
    public ResponseEntity<EntityModel<ApiResponse<List<OfferResponse>>>> getAvailableOffers(Authentication authentication) {
        String username = authenticatedUsername(authentication);
        List<OfferResponse> offers = service.getAvailableOffers(username);

        String message = offers.isEmpty()
                ? "No hay oferta disponible para mostrar"
                : "Consulta de oferta exitosa";

        ApiResponse<List<OfferResponse>> body = new ApiResponse<>(
                message,
                offers,
                TraceIdUtil.currentTraceId()
        );

        EntityModel<ApiResponse<List<OfferResponse>>> model = EntityModel.of(
                body,
                Link.of(ApiPaths.API_V1 + "/offers").withSelfRel(),
                Link.of(ApiPaths.PROTECTED + "/customer-booking/bootstrap").withRel("customer-booking-bootstrap")
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
