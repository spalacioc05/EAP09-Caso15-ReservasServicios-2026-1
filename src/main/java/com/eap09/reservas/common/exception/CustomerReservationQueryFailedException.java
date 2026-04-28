package com.eap09.reservas.common.exception;

public class CustomerReservationQueryFailedException extends ApiException {

    public CustomerReservationQueryFailedException(String message) {
        super("CUSTOMER_BOOKING_QUERY_FAILED", message);
    }
}