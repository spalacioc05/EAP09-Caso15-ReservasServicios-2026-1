package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.common.response.ApiResponse;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.config.ApiPaths;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityRequest;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.application.CustomerBookingAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/services/offer/availability")
@Tag(name = "Customer Booking")
public class CustomerBookingAvailabilityController {

    private final CustomerBookingAvailabilityService service;

    public CustomerBookingAvailabilityController(CustomerBookingAvailabilityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Consultar horarios y cupos disponibles")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailability(
            @Valid @RequestBody AvailabilityRequest request) {

        List<AvailabilityResponse> data =
                service.getAvailability(request.idServicio(), request.fecha());


        ApiResponse<List<AvailabilityResponse>> body = new ApiResponse<>(
                "Consulta de disponibilidad exitosa",
                data,
                TraceIdUtil.currentTraceId()
        );

        return ResponseEntity.ok(body);
    }
}