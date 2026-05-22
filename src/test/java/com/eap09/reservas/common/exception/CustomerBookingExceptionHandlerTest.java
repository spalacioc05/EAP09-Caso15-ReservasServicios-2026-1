package com.eap09.reservas.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eap09.reservas.common.response.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CustomerBookingExceptionHandlerTest {

    private final CustomerBookingExceptionHandler handler = new CustomerBookingExceptionHandler();

    @AfterEach
    void clearTraceId() {
        MDC.clear();
    }

    @Test
    void shouldHandleReservationFinalizationFailure() {
        MDC.put("traceId", "trace-finalization");

        ResponseEntity<ErrorResponse> response = handler.handleReservationFinalizationFailed(
                new ReservationFinalizationFailedException("No fue posible finalizar la reserva")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("RESERVATION_FINALIZATION_FAILED", response.getBody().errorCode());
        assertEquals("No fue posible finalizar la reserva", response.getBody().message());
        assertEquals("trace-finalization", response.getBody().traceId());
    }

    @Test
    void shouldHandleReservationCancellationFailure() {
        MDC.put("traceId", "trace-cancellation");

        ResponseEntity<ErrorResponse> response = handler.handleReservationCancellationFailed(
                new ReservationCancellationFailedException("No fue posible cancelar la reserva")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("RESERVATION_CANCELLATION_FAILED", response.getBody().errorCode());
        assertEquals("No fue posible cancelar la reserva", response.getBody().message());
        assertEquals("trace-cancellation", response.getBody().traceId());
    }
}