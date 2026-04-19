package com.eap09.reservas.common.exception;

public class SessionNotActiveException extends RuntimeException {

    public SessionNotActiveException(String message) {
        super(message);
    }
}
