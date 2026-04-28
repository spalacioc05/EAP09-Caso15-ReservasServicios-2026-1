package com.eap09.reservas.common.exception;

public class ServiceStatusAlreadySetException extends ApiException {

    public ServiceStatusAlreadySetException(String message) {
        super("SERVICE_STATUS_ALREADY_SET", message);
    }
}