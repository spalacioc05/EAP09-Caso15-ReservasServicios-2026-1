package com.eap09.reservas.common.exception;

public class ProviderReservationQueryFailedException extends ApiException {

    public ProviderReservationQueryFailedException(String message) {
        super("PROVIDER_BOOKING_QUERY_FAILED", message);
    }
}