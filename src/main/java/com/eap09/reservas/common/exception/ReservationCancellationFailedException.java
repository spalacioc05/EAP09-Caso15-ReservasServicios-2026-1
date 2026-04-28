package com.eap09.reservas.common.exception;

public class ReservationCancellationFailedException extends ApiException {

    public ReservationCancellationFailedException(String message) {
        super("RESERVATION_CANCELLATION_FAILED", message);
    }
}