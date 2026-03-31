package com.eap09.reservas.common.exception;

public class ServiceNameAlreadyExistsException extends RuntimeException {

    public ServiceNameAlreadyExistsException(String message) {
        super(message);
    }
}