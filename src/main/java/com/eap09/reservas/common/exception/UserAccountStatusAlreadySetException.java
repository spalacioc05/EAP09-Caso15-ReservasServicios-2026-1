package com.eap09.reservas.common.exception;

public class UserAccountStatusAlreadySetException extends RuntimeException {

    public UserAccountStatusAlreadySetException(String message) {
        super(message);
    }
}