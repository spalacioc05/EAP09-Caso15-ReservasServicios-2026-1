package com.eap09.reservas.common.exception;

public class ServiceStatusChangeFailedException extends ApiException {

    public ServiceStatusChangeFailedException(String message) {
        super("SERVICE_STATUS_CHANGE_FAILED", message);
    }
}