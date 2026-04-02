package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.application.CustomerBookingOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/services/offer")
@Tag(name = "Customer Booking")
public class CustomerBookingController {

    private final CustomerBookingOfferService service;

    public CustomerBookingController(CustomerBookingOfferService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar oferta disponible")
    public ResponseEntity<ApiResponse<List<OfferResponse>>> getAvailableOffers() {

        List<OfferResponse> offers = service.getAvailableServices();

        ApiResponse<List<OfferResponse>> body = new ApiResponse<>(
                "Consulta de oferta exitosa",
                offers,
                TraceIdUtil.currentTraceId()
        );

        return ResponseEntity.ok(body);
    }
}