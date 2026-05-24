package com.eap09.reservas.common.exception;

public class UserAccountStatusUpdateFailedException extends RuntimeException {

    public UserAccountStatusUpdateFailedException(String message) {
        super(message);
    }
}