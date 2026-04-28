package com.eap09.reservas.common.exception;

public class ReservationFinalizationFailedException extends ApiException {

    public ReservationFinalizationFailedException(String message) {
        super("RESERVATION_FINALIZATION_FAILED", message);
    }
}