package com.eap09.reservas.common.exception;

public class TemporaryAccessRestrictedException extends RuntimeException {

    public TemporaryAccessRestrictedException(String message) {
        super(message);
    }
}
