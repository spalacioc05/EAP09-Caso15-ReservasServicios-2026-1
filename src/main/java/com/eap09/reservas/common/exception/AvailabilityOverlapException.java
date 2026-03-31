package com.eap09.reservas.common.exception;

public class AvailabilityOverlapException extends RuntimeException {

    public AvailabilityOverlapException(String message) {
        super(message);
    }
}