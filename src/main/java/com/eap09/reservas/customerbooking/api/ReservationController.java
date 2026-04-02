package com.eap09.reservas.customerbooking.api;

import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.application.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.eap09.reservas.config.ApiPaths;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/reservas")
@Tag(name = "Customer Booking")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Reservar")
    public ResponseEntity<CreateReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {

        CreateReservationResponse response = reservationService.createReservation(request);

        return ResponseEntity.ok(response);
    }
}