package com.eap09.reservas.common.exception;

public class ServiceInactivationBlockedException extends ApiException {

    public ServiceInactivationBlockedException(String message) {
        super("SERVICE_HAS_ACTIVE_RESERVATIONS", message);
    }
}