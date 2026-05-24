package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomerBookingExceptionHandler extends AbstractErrorResponseHandler {

    @ExceptionHandler(OfferQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleOfferQueryFailed(OfferQueryFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "OFFER_QUERY_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(AvailabilityQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleAvailabilityQueryFailed(AvailabilityQueryFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "AVAILABILITY_QUERY_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleReservationConflict(ReservationConflictException ex) {
        return response(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ReservationCreationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationCreationFailed(ReservationCreationFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION_CREATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(ProviderReservationQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleProviderReservationQueryFailed(ProviderReservationQueryFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "PROVIDER_BOOKING_QUERY_FAILED", ex.getMessage());
    }

    @ExceptionHandler(CustomerReservationQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerReservationQueryFailed(CustomerReservationQueryFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "CUSTOMER_BOOKING_QUERY_FAILED", ex.getMessage());
    }

    @ExceptionHandler(ReservationFinalizationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationFinalizationFailed(ReservationFinalizationFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION_FINALIZATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(ReservationCancellationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationCancellationFailed(ReservationCancellationFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION_CANCELLATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(ReservationReschedulingFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationReschedulingFailed(ReservationReschedulingFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION_RESCHEDULING_FAILED", ex.getMessage());
    }
}