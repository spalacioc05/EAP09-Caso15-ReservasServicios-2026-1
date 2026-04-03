package com.eap09.reservas.common.exception;

public class ReservationConflictException extends RuntimeException {

    private final String errorCode;

    public ReservationConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
