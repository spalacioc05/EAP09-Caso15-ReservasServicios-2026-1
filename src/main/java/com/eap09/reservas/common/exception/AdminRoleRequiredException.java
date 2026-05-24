package com.eap09.reservas.common.exception;

public class AdminRoleRequiredException extends RuntimeException {

    public AdminRoleRequiredException(String message) {
        super(message);
    }
}